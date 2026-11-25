package farmbot.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import farmbot.control.Controller;
import farmbot.model.Permission;
import farmbot.navigation.DetectPlantStrategy;
import farmbot.navigation.FollowLineStrategy;
import farmbot.navigation.LaneChangeStrategy;
import farmbot.navigation.ManualStrategy;
import farmbot.navigation.SearchStrategy;
import farmbot.users.User;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class StrategyHandler implements HttpHandler {
    private final Controller controller;
    private final SessionManager sessions;

    public StrategyHandler(Controller controller, SessionManager sessions) {
        this.controller = controller;
        this.sessions = sessions;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            User user = WebUtil.requireAuth(exchange, sessions, Permission.START_STOP);
            if (user == null) {
                return;
            }
            Map<String, String> body = WebUtil.readJsonBody(exchange);
            String strategy = body.getOrDefault("strategy", "");

            switch (strategy) {
                case "FOLLOW_LINE" -> {
                    controller.setStrategy(new FollowLineStrategy());
                    WebUtil.sendJson(exchange, 200, Map.of());
                }
                case "DETECT_PLANT" -> {
                    controller.setStrategy(new DetectPlantStrategy());
                    WebUtil.sendJson(exchange, 200, Map.of());
                }
                case "LANE_CHANGE" -> {
                    controller.setStrategy(new LaneChangeStrategy());
                    WebUtil.sendJson(exchange, 200, Map.of());
                }
                case "SEARCH" -> {
                    controller.setStrategy(new SearchStrategy());
                    WebUtil.sendJson(exchange, 200, Map.of());
                }
                case "MANUAL" -> handleManual(exchange);
                default -> WebUtil.sendJson(exchange, 400, Map.of("error", "unknown strategy: " + strategy));
            }
        } finally {
            exchange.close();
        }
    }

    /**
     * Manual control is a deliberate stub (PSEUDOCODE.md's ManualStrategy).
     * Rather than leaving the bot parked in a non-functional strategy, this
     * probes decide() directly and turns the caught UnsupportedOperationException
     * into a clean 4xx response - a live exception demo at the HTTP boundary,
     * distinct from Controller.tick()'s own internal catch for the same class.
     */
    private void handleManual(HttpExchange exchange) throws IOException {
        try {
            new ManualStrategy().decide(List.of());
            WebUtil.sendJson(exchange, 200, Map.of());
        } catch (UnsupportedOperationException e) {
            WebUtil.sendJson(exchange, 400, Map.of("error", e.getMessage()));
        }
    }
}
