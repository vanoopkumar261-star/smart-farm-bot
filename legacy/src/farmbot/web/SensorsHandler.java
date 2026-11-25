package farmbot.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import farmbot.control.Controller;
import farmbot.hardware.Sensor;
import farmbot.model.SensorReading;
import farmbot.users.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SensorsHandler implements HttpHandler {
    private final Controller controller;
    private final SessionManager sessions;

    public SensorsHandler(Controller controller, SessionManager sessions) {
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
            List<Object> sensors = new ArrayList<>();
            for (Sensor sensor : controller.getSensors()) {
                List<SensorReading> history = sensor.getHistory();
                String display = history.isEmpty() ? "no reading yet" : history.get(history.size() - 1).getDisplay();
                sensors.add(Map.of(
                    "id", sensor.getId(),
                    "name", sensor.getName(),
                    "type", sensor.getClass().getSimpleName(),
                    "description", sensor.getDescription(),
                    "display", display,
                    "active", sensor.isActive()
                ));
            }
            WebUtil.sendJson(exchange, 200, sensors);
        } finally {
            exchange.close();
        }
    }
}
