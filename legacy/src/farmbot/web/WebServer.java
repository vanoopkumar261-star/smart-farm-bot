package farmbot.web;

import com.sun.net.httpserver.HttpServer;
import farmbot.control.Controller;
import farmbot.control.LineFollowerBot;
import farmbot.hardware.DriveTrain;
import farmbot.hardware.Esp32CamSensor;
import farmbot.hardware.LineSensor;
import farmbot.hardware.ObstacleSensor;
import farmbot.hardware.Sensor;
import farmbot.hardware.SoilMoistureSensor;
import farmbot.sim.SimulatedSource;
import farmbot.sim.Track;
import farmbot.users.UserRegistry;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Serves the dashboard and its API. Builds one shared Robot/Controller/
 * UserRegistry instance that every handler calls into - the same domain
 * classes the console demo (farmbot.main.Main) uses, not a parallel copy.
 * Run from the project root so the relative "public" path resolves.
 */
public class WebServer {
    private static final int PORT = 8080;

    public static void main(String[] args) throws Exception {
        Track track = new Track("tracks/track1.txt");
        SimulatedSource simSource = new SimulatedSource(track);
        LineSensor lineSensor = new LineSensor("line-1", simSource);
        ObstacleSensor obstacleSensor = new ObstacleSensor("obstacle-1", 30.0);
        SoilMoistureSensor soilSensor = new SoilMoistureSensor("soil-1");
        soilSensor.setMoistureLevel(22); // seeded low so the advisory panel has something to show
        Esp32CamSensor camSensor = new Esp32CamSensor("cam-1");
        List<Sensor> sensors = List.of(lineSensor, obstacleSensor, soilSensor, camSensor);

        DriveTrain driveTrain = new DriveTrain();
        Controller controller = new Controller(sensors, driveTrain);
        LineFollowerBot bot = new LineFollowerBot("bot-web", controller);
        bot.start();

        UserRegistry registry = new UserRegistry();
        SessionManager sessions = new SessionManager();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new StaticFileHandler("public"));
        server.createContext("/api/register", new AuthHandler(registry, sessions));
        server.createContext("/api/login", new AuthHandler(registry, sessions));
        server.createContext("/api/logout", new AuthHandler(registry, sessions));
        server.createContext("/api/status", new StatusHandler(controller, sessions));
        server.createContext("/api/sensors", new SensorsHandler(controller, sessions));
        server.createContext("/api/faults", new FaultsHandler(controller, sessions));
        server.createContext("/api/recommendations", new RecommendationsHandler(controller, sessions));
        server.createContext("/api/strategy", new StrategyHandler(controller, sessions));
        server.createContext("/api/tick", new TickHandler(controller, sessions, simSource));
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        System.out.println("Smart Farm Bot web server running at http://localhost:" + PORT + "/login.html");
    }
}
