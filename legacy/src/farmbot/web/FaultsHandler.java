package farmbot.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import farmbot.control.Controller;
import farmbot.model.Fault;
import farmbot.users.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FaultsHandler implements HttpHandler {
    private final Controller controller;
    private final SessionManager sessions;

    public FaultsHandler(Controller controller, SessionManager sessions) {
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
            List<Object> faults = new ArrayList<>();
            for (Fault fault : controller.getFaults()) {
                faults.add(Map.of(
                    "sensorId", fault.getSensorId(),
                    "message", fault.getMessage(),
                    "critical", fault.isCritical(),
                    "timestamp", fault.getTimestamp()
                ));
            }
            WebUtil.sendJson(exchange, 200, faults);
        } finally {
            exchange.close();
        }
    }
}
