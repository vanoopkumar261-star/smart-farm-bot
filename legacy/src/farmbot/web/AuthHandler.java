package farmbot.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import farmbot.exceptions.AuthenticationException;
import farmbot.model.Permission;
import farmbot.users.User;
import farmbot.users.UserRegistry;

import java.io.IOException;
import java.util.Map;

public class AuthHandler implements HttpHandler {
    private final UserRegistry registry;
    private final SessionManager sessions;

    public AuthHandler(UserRegistry registry, SessionManager sessions) {
        this.registry = registry;
        this.sessions = sessions;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        try {
            if (path.endsWith("/register")) {
                handleRegister(exchange);
            } else if (path.endsWith("/login")) {
                handleLogin(exchange);
            } else if (path.endsWith("/logout")) {
                handleLogout(exchange);
            } else {
                WebUtil.sendJson(exchange, 404, Map.of("error", "not found"));
            }
        } finally {
            exchange.close();
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        Map<String, String> body = WebUtil.readJsonBody(exchange);
        String username = body.getOrDefault("username", "");
        String password = body.getOrDefault("password", "");
        String role = body.getOrDefault("role", "FARMER");
        try {
            User user = registry.register(username, password, role);
            WebUtil.sendJson(exchange, 200, Map.of(
                "username", user.getUsername(),
                "role", user instanceof farmbot.users.Technician ? "TECHNICIAN" : "FARMER"
            ));
        } catch (IllegalArgumentException e) {
            WebUtil.sendJson(exchange, 400, Map.of("error", e.getMessage()));
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        Map<String, String> body = WebUtil.readJsonBody(exchange);
        String username = body.getOrDefault("username", "");
        String password = body.getOrDefault("password", "");
        try {
            User user = registry.authenticate(username, password);
            String token = sessions.createSession(user);
            String role = user.getPermissions().contains(Permission.CALIBRATE_SENSORS) ? "TECHNICIAN" : "FARMER";
            WebUtil.sendJson(exchange, 200, Map.of(
                "token", token,
                "username", user.getUsername(),
                "role", role
            ));
        } catch (AuthenticationException e) {
            WebUtil.sendJson(exchange, 401, Map.of("error", e.getMessage()));
        }
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        String token = exchange.getRequestHeaders().getFirst("X-Auth-Token");
        sessions.invalidate(token);
        WebUtil.sendJson(exchange, 200, Map.of());
    }
}
