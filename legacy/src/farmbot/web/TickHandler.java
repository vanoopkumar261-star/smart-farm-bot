package farmbot.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import farmbot.control.Controller;
import farmbot.model.Permission;
import farmbot.sim.SimulatedSource;
import farmbot.users.User;

import java.io.IOException;
import java.util.Map;

public class TickHandler implements HttpHandler {
    private final Controller controller;
    private final SessionManager sessions;
    private final SimulatedSource simulatedSource;

    public TickHandler(Controller controller, SessionManager sessions, SimulatedSource simulatedSource) {
        this.controller = controller;
        this.sessions = sessions;
        this.simulatedSource = simulatedSource;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            User user = WebUtil.requireAuth(exchange, sessions, Permission.START_STOP);
            if (user == null) {
                return;
            }
            controller.tick();
            if (simulatedSource != null) {
                simulatedSource.advance();
            }
            WebUtil.sendJson(exchange, 200, Map.of());
        } finally {
            exchange.close();
        }
    }
}
