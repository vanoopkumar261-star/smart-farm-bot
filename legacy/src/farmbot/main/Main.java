package farmbot.main;

import farmbot.exceptions.AuthenticationException;
import farmbot.exceptions.InvalidSpeedException;
import farmbot.exceptions.MotorStallException;
import farmbot.control.Controller;
import farmbot.control.LineFollowerBot;
import farmbot.hardware.DriveTrain;
import farmbot.hardware.Esp32CamSensor;
import farmbot.hardware.LineSensor;
import farmbot.hardware.ObstacleSensor;
import farmbot.hardware.Sensor;
import farmbot.hardware.SoilMoistureSensor;
import farmbot.model.BotState;
import farmbot.model.Command;
import farmbot.model.Direction;
import farmbot.model.Fault;
import farmbot.model.Recommendation;
import farmbot.navigation.DetectPlantStrategy;
import farmbot.navigation.LaneChangeStrategy;
import farmbot.navigation.ManualStrategy;
import farmbot.rules.RecommendationEngine;
import farmbot.sim.ManualSource;
import farmbot.sim.SimulatedSource;
import farmbot.sim.Track;
import farmbot.users.User;
import farmbot.users.UserRegistry;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("=================================");
        System.out.println("        SMART FARM BOT");
        System.out.println("      OOP LINE FOLLOWER");
        System.out.println("=================================");

        ManualSource manualSource = new ManualSource();
        LineSensor lineSensor = new LineSensor("line-1", manualSource);
        DriveTrain driveTrain = new DriveTrain();
        SoilMoistureSensor soilSensor = new SoilMoistureSensor("soil-1");
        soilSensor.setMoistureLevel(65);
        ObstacleSensor obstacleSensor = new ObstacleSensor("obstacle-1", 30.0);
        Esp32CamSensor camSensor = new Esp32CamSensor("cam-1");
        List<Sensor> manualSensors = List.of(lineSensor, obstacleSensor, soilSensor, camSensor);
        Controller manualController = new Controller(manualSensors, driveTrain);
        LineFollowerBot robot = new LineFollowerBot("bot-1", manualController);
        robot.start();

        System.out.println("\n--- OOP SENSOR DEMONSTRATION ---");
        for (Sensor sensor : manualSensors) {
            sensor.readData();
        }

        System.out.println("\n--- EXCEPTION HANDLING DEMONSTRATION ---");
        try {
            driveTrain.getLeftMotor().setSpeed(150);
        } catch (InvalidSpeedException e) {
            System.out.println("Caught InvalidSpeedException: " + e.getMessage());
        }
        driveTrain.getLeftMotor().setStalled(true);
        try {
            driveTrain.execute(new Command(Direction.FORWARD, 50));
        } catch (MotorStallException | InvalidSpeedException e) {
            System.out.println("Caught " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        driveTrain.getLeftMotor().setStalled(false);
        driveTrain.stop();

        System.out.println("\n--- USER REGISTRY DEMONSTRATION ---");
        UserRegistry registry = new UserRegistry();
        User farmer = registry.register("anoop", "secret123", "FARMER");
        User technician = registry.register("tech1", "secret456", "TECHNICIAN");
        System.out.println(farmer.getUsername() + " (Farmer) permissions: " + farmer.getPermissions());
        System.out.println(farmer.getUsername() + " dashboard view: " + farmer.getDashboardView());
        System.out.println(technician.getUsername() + " (Technician) permissions: " + technician.getPermissions());
        System.out.println(technician.getUsername() + " dashboard view: " + technician.getDashboardView());
        try {
            registry.authenticate("anoop", "wrong-password");
        } catch (AuthenticationException e) {
            System.out.println("Caught AuthenticationException: " + e.getMessage());
        }
        try {
            registry.register("anoop", "another", "FARMER");
        } catch (IllegalArgumentException e) {
            System.out.println("Caught IllegalArgumentException: " + e.getMessage());
        }

        System.out.println("\n--- AUTONOMOUS STRATEGY DEMONSTRATION ---");
        Controller autorunController = runAutorunDemo();

        System.out.println("\n--- RECOMMENDATION ENGINE DEMONSTRATION ---");
        RecommendationEngine engine = new RecommendationEngine();
        BotState lowMoistureState = new BotState(
            18.0, // seeded low on purpose to trigger LowMoistureRule
            autorunController.getObstacleLog(),
            autorunController.getLineLostCount(),
            autorunController.getOdometer().getTotalDistance()
        );
        List<Recommendation> recommendations = engine.runAll(lowMoistureState);
        if (recommendations.isEmpty()) {
            System.out.println("No recommendations.");
        } else {
            for (Recommendation r : recommendations) {
                System.out.println("- " + r.getMessage());
            }
        }

        System.out.println("\n--- MANUAL CONTROL (interactive) ---");
        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        while (running) {
            System.out.println("\nEnter sensor values:");
            System.out.println("Format: LEFT CENTER RIGHT");
            System.out.println("Example: 0 1 0 = Forward");
            System.out.println("Example: 1 0 0 = Turn Left");
            System.out.println("Example: 0 0 1 = Turn Right");
            System.out.println("Example: 0 0 0 = Line Lost");
            System.out.println("Enter 9 9 9 to stop robot.");
            int left = scanner.nextInt();
            int center = scanner.nextInt();
            int right = scanner.nextInt();
            if (left == 9 && center == 9 && right == 9) {
                robot.stop();
                running = false;
            }
            else if (
                (left == 0 || left == 1) &&
                (center == 0 || center == 1) &&
                (right == 0 || right == 1)
            ) {
                manualSource.inject(left, center, right);
                robot.move();
            }
            else {
                System.out.println("Invalid sensor values!");
            }
        }
        scanner.close();
        System.out.println("\nProgram terminated.");
    }

    private static Controller runAutorunDemo() {
        DriveTrain autorunDrive = new DriveTrain();
        Track track;
        try {
            track = new Track("tracks/track1.txt");
        } catch (IOException e) {
            System.out.println("Track file not found, skipping autorun demo: " + e.getMessage());
            return new Controller(List.of(), autorunDrive);
        }
        SimulatedSource simSource = new SimulatedSource(track);
        LineSensor autorunLine = new LineSensor("line-auto", simSource);
        List<Sensor> autorunSensors = List.of(autorunLine);
        Controller controller = new Controller(autorunSensors, autorunDrive);
        LineFollowerBot autorunBot = new LineFollowerBot("bot-auto", controller);
        autorunBot.start();

        for (int i = 0; i < track.size(); i++) {
            autorunBot.move();
            System.out.println(
                "Tick " + i + ": status=" + controller.getStatus() +
                " strategy=" + controller.getStrategy().getName()
            );
            simSource.advance();
        }

        System.out.println("\n-- Strategy swap demonstration --");
        controller.setStrategy(new LaneChangeStrategy());
        for (int i = 0; i < 3; i++) {
            controller.tick();
            System.out.println("LaneChange tick " + i + ": strategy=" + controller.getStrategy().getName());
        }

        controller.setStrategy(new DetectPlantStrategy());
        controller.tick();
        System.out.println("DetectPlant tick: strategy=" + controller.getStrategy().getName());

        controller.setStrategy(new ManualStrategy());
        controller.tick();
        System.out.println("Manual tick: strategy=" + controller.getStrategy().getName() +
            " (should log a fault, not crash)");
        for (Fault fault : controller.getFaults()) {
            System.out.println("  Fault: [" + fault.getSensorId() + "] " + fault.getMessage());
        }

        return controller;
    }
}
