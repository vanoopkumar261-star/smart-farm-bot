package farmbot.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import farmbot.control.Controller;
import farmbot.users.User;

import java.io.IOException;
import java.util.Map;

public class StatusHandler implements HttpHandler {
    private final Controller controller;
    private final SessionManager sessions;

    public StatusHandler(Controller controller, SessionManager sessions) {
        this.controller = controller;
        this.sessions = sessions;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            User user = WebUtil.requireAuth(exchange, sessions, null);
            if (user == null) {
                return;
            }
            WebUtil.sendJson(exchange, 200, Map.of(
                "status", controller.getStatus().name(),
                "strategy", controller.getStrategy().getName(),
                "distance", controller.getOdometer().getTotalDistance(),
                "obstaclesDetected", controller.getObstacleLog().countDetected(),
                "obstacleCollisions", controller.getObstacleLog().countCollisions()
            ));
        } finally {
            exchange.close();
        }
    }
}
