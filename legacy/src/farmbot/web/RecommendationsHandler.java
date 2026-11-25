package farmbot.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import farmbot.control.Controller;
import farmbot.hardware.Sensor;
import farmbot.hardware.SoilMoistureSensor;
import farmbot.model.BotState;
import farmbot.model.Recommendation;
import farmbot.rules.RecommendationEngine;
import farmbot.users.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RecommendationsHandler implements HttpHandler {
    private final Controller controller;
    private final SessionManager sessions;
    private final RecommendationEngine engine = new RecommendationEngine();

    public RecommendationsHandler(Controller controller, SessionManager sessions) {
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
            double moisture = findMoisturePct();
            BotState state = new BotState(
                moisture,
                controller.getObstacleLog(),
                controller.getLineLostCount(),
                controller.getOdometer().getTotalDistance()
            );
            List<Object> recommendations = new ArrayList<>();
            for (Recommendation r : engine.runAll(state)) {
                recommendations.add(Map.of("message", r.getMessage(), "timestamp", r.getTimestamp()));
            }
            WebUtil.sendJson(exchange, 200, recommendations);
        } finally {
            exchange.close();
        }
    }

    private double findMoisturePct() {
        for (Sensor sensor : controller.getSensors()) {
            if (sensor instanceof SoilMoistureSensor soil) {
                return soil.getMoistureLevel();
            }
        }
        return 100.0; // no soil sensor on this bot - never triggers the low-moisture advisory
    }
}
