package farmbot.control;

import farmbot.model.BotStatus;

public class Robot {
    protected String id;
    protected Controller controller;
    protected BotStatus status;

    public Robot(String id, Controller controller) {
        this.id = id;
        this.controller = controller;
        this.status = BotStatus.IDLE;
    }

    public void start() {
        status = BotStatus.RUNNING;
        System.out.println("Robot STARTED");
    }

    public void stop() {
        status = BotStatus.HALTED;
        controller.getDriveTrain().cutPower();
        System.out.println("Robot STOPPED");
    }

    public void move() {
        if (status == BotStatus.RUNNING || status == BotStatus.SEARCHING) {
            controller.tick();
            status = controller.getStatus();
        } else {
            System.out.println("Robot is not running.");
        }
    }

    public BotStatus getStatus() {
        return status;
    }

    public Controller getController() {
        return controller;
    }
}
