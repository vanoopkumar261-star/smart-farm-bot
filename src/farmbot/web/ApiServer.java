package farmbot.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import farmbot.concurrency.FarmBotRuntime;
import farmbot.core.SmartFarmBot;
import farmbot.data.FaultRecord;
import farmbot.data.ObstacleEvent;
import farmbot.data.Reading;
import farmbot.data.SeedLoader;
import farmbot.data.SensorRepository;
import farmbot.data.SqliteSensorRepository;
import farmbot.exception.AuthenticationException;
import farmbot.exception.ModuleNotReadyException;
import farmbot.model.SensorType;
import farmbot.module.MissionModule;
import farmbot.nav.DetectPlantStrategy;
import farmbot.nav.FollowLineStrategy;
import farmbot.nav.LaneChangeStrategy;
import farmbot.nav.NavigationStrategy;
import farmbot.nav.SearchStrategy;
import farmbot.rules.Recommendation;
import farmbot.rules.RecommendationEngine;
import farmbot.rules.RuleContext;
import farmbot.sensor.Sensor;
import farmbot.user.AuthService;
import farmbot.user.Role;
import farmbot.user.User;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

/**
 * The HTTP layer: a thin PRESENTATION adapter over the exact same domain objects the
 * console engine uses. It adds no business logic of its own - every endpoint just
 * reads the live {@link SmartFarmBot} or the {@link SensorRepository} and renders the
 * answer as JSON with {@link JsonUtil}.
 *
 * Built only on the JDK's com.sun.net.httpserver.HttpServer and hand-written JSON, as
 * the spec requires: no Spring, no servlet container, no JSON library.
 *
 * Routing: everything under /api/ is handled here; every other path serves a static
 * file from public/. Protected endpoints require a valid X-Auth-Token header; only
 * /api/login and /api/register are open.
 */
public final class ApiServer {

    private static final int DEFAULT_HISTORY = 60;
    private static final int MAX_HISTORY = 500;

    private final SmartFarmBot bot;
    private final SensorRepository repository;
    private final AuthService authService;
    private final SessionManager sessions = new SessionManager();
    private final RecommendationEngine recommendations = RecommendationEngine.withDefaultRules();
    private final Path publicDir = Path.of("public");
    private final int port;

    private HttpServer server;

    public ApiServer(SmartFarmBot bot, SensorRepository repository, AuthService authService, int port) {
        this.bot = bot;
        this.repository = repository;
        this.authService = authService;
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handleStatic);
        server.createContext("/api/", this::handleApi);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("[web] dashboard at http://localhost:" + port + "/login.html");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // ---- API routing ------------------------------------------------------

    private void handleApi(HttpExchange exchange) {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            // Open endpoints (no session needed).
            if (path.equals("/api/login") && method.equals("POST")) {
                handleLogin(exchange);
                return;
            }
            if (path.equals("/api/register") && method.equals("POST")) {
                handleRegister(exchange);
                return;
            }

            // Everything else needs a valid session.
            Optional<User> user = sessions.lookup(exchange.getRequestHeaders().getFirst("X-Auth-Token"));
            if (user.isEmpty()) {
                sendError(exchange, 401, "Not logged in");
                return;
            }

            switch (path) {
                case "/api/logout" -> handleLogout(exchange);
                case "/api/status" -> requireGet(exchange, method, this::handleStatus);
                case "/api/sensors" -> requireGet(exchange, method, this::handleSensors);
                case "/api/modules" -> requireGet(exchange, method, this::handleModules);
                case "/api/history" -> requireGet(exchange, method, this::handleHistory);
                case "/api/obstacles" -> requireGet(exchange, method, this::handleObstacles);
                case "/api/faults" -> requireGet(exchange, method, this::handleFaults);
                case "/api/recommendations" -> requireGet(exchange, method, this::handleRecommendations);
                case "/api/strategy" -> {
                    if (method.equals("POST")) {
                        handleStrategy(exchange);
                    } else {
                        sendError(exchange, 405, "Use POST");
                    }
                }
                default -> sendError(exchange, 404, "Unknown endpoint " + path);
            }
        } catch (RuntimeException e) {
            // A handler bug must become a clean 500, never a dropped connection.
            trySendError(exchange, 500, "Server error: " + e.getMessage());
            System.err.println("[web] handler failed: " + e);
        } finally {
            exchange.close();
        }
    }

    private interface ApiAction {
        void run(HttpExchange exchange);
    }

    private void requireGet(HttpExchange exchange, String method, ApiAction action) {
        if (method.equals("GET")) {
            action.run(exchange);
        } else {
            sendError(exchange, 405, "Use GET");
        }
    }

    // ---- auth endpoints ---------------------------------------------------

    private void handleLogin(HttpExchange exchange) {
        Map<String, String> body = readJsonBody(exchange);
        try {
            User user = authService.authenticate(body.get("username"), body.get("password"));
            String token = sessions.open(user);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("token", token);
            out.put("username", user.getUsername());
            out.put("role", user.getRole().name());
            out.put("canAccessHardware", user.canAccessHardware());
            sendJson(exchange, 200, out);
        } catch (AuthenticationException e) {
            sendError(exchange, 401, e.getMessage());
        }
    }

    private void handleRegister(HttpExchange exchange) {
        Map<String, String> body = readJsonBody(exchange);
        try {
            Role role = Role.fromText(body.getOrDefault("role", "FARMER"));
            User user = authService.register(body.get("username"), body.get("password"), role);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("username", user.getUsername());
            out.put("role", user.getRole().name());
            sendJson(exchange, 201, out);
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, e.getMessage());
        }
    }

    private void handleLogout(HttpExchange exchange) {
        sessions.close(exchange.getRequestHeaders().getFirst("X-Auth-Token"));
        sendJson(exchange, 200, Map.of("ok", true));
    }

    // ---- read endpoints ---------------------------------------------------

    private void handleStatus(HttpExchange exchange) {
        Map<SensorType, Reading> latest = bot.latestSnapshot();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", bot.getStatus().name());
        out.put("statusDescription", bot.getStatus().getDescription());
        out.put("strategy", bot.getStrategyName());
        out.put("speed", bot.getDriveTrain().getCurrentSpeed());
        out.put("distanceMeters", round1(bot.getOdometer().getDistanceMetres()));
        out.put("battery", valueOrNull(latest.get(SensorType.BATTERY)));
        out.put("simulated", true);
        sendJson(exchange, 200, out);
    }

    private void handleSensors(HttpExchange exchange) {
        Map<SensorType, Reading> latest = bot.latestSnapshot();
        List<Object> list = new ArrayList<>();
        for (Sensor sensor : bot.getSensors()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", sensor.getId());
            entry.put("name", sensor.getName());
            entry.put("type", sensor.getType().getDisplayName());
            entry.put("critical", sensor.isCritical());
            entry.put("connected", sensor.isConnected());
            Reading reading = latest.get(sensor.getType());
            if (reading != null) {
                entry.put("value", round2(reading.getValue()));
                entry.put("unit", reading.getUnit());
                entry.put("raw", reading.getRaw());
                entry.put("status", reading.getLabel().name());
                entry.put("level", reading.getStatus().name());
                entry.put("recordedAt", reading.getRecordedAtText());
            } else {
                entry.put("value", null);
                entry.put("unit", sensor.getType().getUnit());
                entry.put("raw", "");
                entry.put("status", "INACTIVE");
                entry.put("level", "INACTIVE");
                entry.put("recordedAt", null);
            }
            list.add(entry);
        }
        sendJson(exchange, 200, Map.of("sensors", list, "simulated", true));
    }

    private void handleModules(HttpExchange exchange) {
        List<Object> list = new ArrayList<>();
        for (MissionModule module : bot.getModules()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", module.getId());
            entry.put("name", module.getName());
            entry.put("description", module.getDescription());
            entry.put("state", module.getState().name());
            entry.put("mountedAt", module.getMountedAt() == null
                    ? null : module.getMountedAt().format(Reading.TIME_FORMAT));
            try {
                Reading reading = module.read();
                entry.put("value", round2(reading.getValue()));
                entry.put("unit", reading.getUnit());
                entry.put("status", reading.getLabel().name());
            } catch (ModuleNotReadyException notReady) {
                // stub / not-ready module (e.g. the 3D scanner): no reading to show
                entry.put("value", null);
                entry.put("unit", "");
                entry.put("status", module.getState().name());
            }
            list.add(entry);
        }
        sendJson(exchange, 200, Map.of("modules", list, "simulated", true));
    }

    private void handleHistory(HttpExchange exchange) {
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        String sensorId = query.get("sensor");
        if (sensorId == null || sensorId.isBlank()) {
            sendError(exchange, 400, "Missing 'sensor' parameter");
            return;
        }
        int limit = DEFAULT_HISTORY;
        try {
            if (query.containsKey("limit")) {
                limit = Math.min(MAX_HISTORY, Math.max(1, Integer.parseInt(query.get("limit"))));
            }
        } catch (NumberFormatException ignored) {
            // keep the default
        }
        List<Object> rows = new ArrayList<>();
        for (Reading reading : repository.findBySensor(sensorId, limit)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("value", round2(reading.getValue()));
            row.put("unit", reading.getUnit());
            row.put("status", reading.getLabel().name());
            row.put("level", reading.getStatus().name());
            row.put("recordedAt", reading.getRecordedAtText());
            rows.add(row);
        }
        sendJson(exchange, 200, Map.of("sensor", sensorId, "history", rows));
    }

    private void handleObstacles(HttpExchange exchange) {
        List<ObstacleEvent> events = repository.obstacleEvents();
        int detected = 0;
        int undetected = 0;
        List<Object> list = new ArrayList<>();
        for (ObstacleEvent event : events) {
            if (event.wasDetected()) {
                detected++;
            } else {
                undetected++;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("eventTime", event.getEventTimeText());
            entry.put("distanceCm", event.getDistanceCm());
            entry.put("outcome", event.getOutcome());
            list.add(entry);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("detected", detected);
        out.put("undetected", undetected);
        out.put("total", events.size());
        out.put("events", list);
        sendJson(exchange, 200, out);
    }

    private void handleFaults(HttpExchange exchange) {
        List<Object> list = new ArrayList<>();
        for (FaultRecord fault : repository.recentFaults(50)) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("faultTime", fault.getFaultTimeText());
            entry.put("source", fault.getSource());
            entry.put("exception", fault.getException());
            entry.put("message", fault.getMessage());
            list.add(entry);
        }
        sendJson(exchange, 200, Map.of("faults", list));
    }

    private void handleRecommendations(HttpExchange exchange) {
        RuleContext context = new RuleContext(bot.latestSnapshot(), repository.obstacleEvents().size());
        List<Object> list = new ArrayList<>();
        for (Recommendation rec : recommendations.evaluate(context)) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("ruleId", rec.getRuleId());
            entry.put("severity", rec.getSeverity().name());
            entry.put("title", rec.getTitle());
            entry.put("message", rec.getMessage());
            entry.put("action", rec.getAction());
            list.add(entry);
        }
        sendJson(exchange, 200, Map.of("recommendations", list));
    }

    private void handleStrategy(HttpExchange exchange) {
        Map<String, String> body = readJsonBody(exchange);
        String name = body.getOrDefault("strategy", "").trim().toUpperCase();
        NavigationStrategy strategy = switch (name) {
            case FollowLineStrategy.NAME -> new FollowLineStrategy();
            case SearchStrategy.NAME -> new SearchStrategy();
            case LaneChangeStrategy.NAME -> new LaneChangeStrategy();
            case DetectPlantStrategy.NAME -> new DetectPlantStrategy();
            default -> null;
        };
        if (name.equals("MANUAL")) {
            // Manual control is a stub: refuse cleanly with a 4xx instead of letting its
            // UnsupportedOperationException become a 500.
            sendError(exchange, 400, "Manual control is not available from the dashboard");
            return;
        }
        if (strategy == null) {
            sendError(exchange, 400, "Unknown strategy '" + name + "'");
            return;
        }
        bot.setStrategy(strategy);
        sendJson(exchange, 200, Map.of("strategy", strategy.getName()));
    }

    // ---- static files -----------------------------------------------------

    private void handleStatic(HttpExchange exchange) {
        try {
            if (!exchange.getRequestMethod().equals("GET")) {
                sendError(exchange, 405, "Use GET");
                return;
            }
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) {
                path = "/login.html";
            }
            Path file = publicDir.resolve(path.substring(1)).normalize();
            // Path traversal guard: the resolved file must stay inside public/.
            if (!file.startsWith(publicDir.toAbsolutePath().normalize())
                    && !file.toAbsolutePath().normalize().startsWith(publicDir.toAbsolutePath().normalize())) {
                sendError(exchange, 403, "Forbidden");
                return;
            }
            if (!Files.isRegularFile(file)) {
                sendError(exchange, 404, "Not found: " + path);
                return;
            }
            byte[] bytes = Files.readAllBytes(file);
            exchange.getResponseHeaders().set("Content-Type", contentType(file));
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (IOException | RuntimeException e) {
            trySendError(exchange, 500, "Static file error");
            System.err.println("[web] static handler failed: " + e);
        } finally {
            exchange.close();
        }
    }

    private static String contentType(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".css")) return "text/css; charset=utf-8";
        if (name.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (name.endsWith(".json")) return "application/json; charset=utf-8";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".svg")) return "image/svg+xml";
        if (name.endsWith(".woff2")) return "font/woff2";
        if (name.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }

    // ---- small helpers ----------------------------------------------------

    private Map<String, String> readJsonBody(HttpExchange exchange) {
        try (InputStream in = exchange.getRequestBody()) {
            String raw = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return JsonUtil.parseObject(raw);
        } catch (IOException e) {
            return Map.of();
        } catch (IllegalArgumentException badJson) {
            return Map.of(); // malformed body -> treated as missing fields -> 400 downstream
        }
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> params = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return params;
        }
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            if (eq >= 0) {
                params.put(pair.substring(0, eq), pair.substring(eq + 1));
            } else {
                params.put(pair, "");
            }
        }
        return params;
    }

    private void sendJson(HttpExchange exchange, int status, Object body) {
        byte[] bytes = JsonUtil.write(body).getBytes(StandardCharsets.UTF_8);
        try {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (IOException e) {
            System.err.println("[web] could not send response: " + e);
        }
    }

    private void sendError(HttpExchange exchange, int status, String message) {
        sendJson(exchange, status, Map.of("error", message));
    }

    /** Best-effort error send for a handler that failed before responding. */
    private void trySendError(HttpExchange exchange, int status, String message) {
        try {
            sendError(exchange, status, message);
        } catch (RuntimeException ignored) {
            // response may already be committed; nothing more we can do
        }
    }

    private static Object valueOrNull(Reading reading) {
        return reading == null ? null : round2(reading.getValue());
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    // ---- application entry point ------------------------------------------

    /**
     * Boots the whole application: open the database, seed history on first run, build
     * the rover, start the live engine (poller + db-writer + nav-ticker), then start
     * the web server. The FarmBotRuntime installs the clean-shutdown hook.
     */
    public static void main(String[] args) throws IOException {
        SqliteSensorRepository repository = new SqliteSensorRepository("data/farmbot.db");
        repository.initSchema();
        new SeedLoader(repository, Path.of("data", "seed")).importIfEmpty();

        AuthService authService = AuthService.withDemoUsers();
        SmartFarmBot bot = SmartFarmBot.demoRig();

        FarmBotRuntime runtime = new FarmBotRuntime(bot, repository, 1000, 1000);
        runtime.start();

        ApiServer server = new ApiServer(bot, repository, authService, 8080);
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "web-stop"));
    }
}
