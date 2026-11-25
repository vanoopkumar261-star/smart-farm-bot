package farmbot.web;

import com.sun.net.httpserver.HttpExchange;
import farmbot.model.Permission;
import farmbot.users.User;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Shared response/auth helpers so every handler doesn't repeat this logic. */
final class WebUtil {
    private WebUtil() {
    }

    static void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = Json.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    static Map<String, String> readJsonBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            String raw = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return Json.parseFlat(raw);
        }
    }

    /** Returns the authenticated user, or null after already sending a 401/403. */
    static User requireAuth(HttpExchange exchange, SessionManager sessions, Permission required) throws IOException {
        String token = exchange.getRequestHeaders().getFirst("X-Auth-Token");
        User user = sessions.getUser(token);
        if (user == null) {
            sendJson(exchange, 401, Map.of("error", "not authenticated"));
            return null;
        }
        if (required != null && !user.getPermissions().contains(required)) {
            sendJson(exchange, 403, Map.of("error", "insufficient permissions"));
            return null;
        }
        return user;
    }
}
