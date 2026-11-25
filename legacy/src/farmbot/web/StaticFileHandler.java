package farmbot.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class StaticFileHandler implements HttpHandler {
    private static final Map<String, String> CONTENT_TYPES = Map.ofEntries(
        Map.entry(".html", "text/html; charset=utf-8"),
        Map.entry(".css", "text/css; charset=utf-8"),
        Map.entry(".js", "application/javascript; charset=utf-8"),
        Map.entry(".json", "application/json; charset=utf-8"),
        Map.entry(".woff2", "font/woff2"),
        Map.entry(".png", "image/png"),
        Map.entry(".jpg", "image/jpeg"),
        Map.entry(".svg", "image/svg+xml"),
        Map.entry(".ico", "image/x-icon")
    );

    private final Path root;

    public StaticFileHandler(String rootDir) {
        this.root = Path.of(rootDir).toAbsolutePath().normalize();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestPath = exchange.getRequestURI().getPath();
        if (requestPath.contains("..")) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }
        if (requestPath.equals("/") || requestPath.isEmpty()) {
            requestPath = "/login.html";
        }

        Path filePath = root.resolve(requestPath.substring(1)).normalize();
        if (!filePath.startsWith(root) || !Files.isRegularFile(filePath)) {
            byte[] notFound = "Not found".getBytes();
            exchange.sendResponseHeaders(404, notFound.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(notFound);
            }
            return;
        }

        String contentType = CONTENT_TYPES.getOrDefault(extensionOf(filePath), "application/octet-stream");
        byte[] bytes = Files.readAllBytes(filePath);
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String extensionOf(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : "";
    }
}
