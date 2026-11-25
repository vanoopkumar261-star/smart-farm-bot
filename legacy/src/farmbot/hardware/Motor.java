package farmbot.hardware;

import farmbot.exceptions.InvalidSpeedException;
import farmbot.exceptions.MotorStallException;
import farmbot.model.Command;
import farmbot.model.Direction;

public class Motor extends Actuator {
    private int speed;
    private Direction direction;
    private boolean stalled;

    public Motor(String id, String name) {
        super(id, name);
        speed = 0;
        direction = Direction.STOP;
        stalled = false;
    }

    public void setSpeed(int newSpeed) throws InvalidSpeedException {
        if (newSpeed < 0 || newSpeed > 100) {
            throw new InvalidSpeedException("speed must be 0-100, got " + newSpeed);
        }
        this.speed = newSpeed;
    }

    @Override
    public void actuate(Command command) throws MotorStallException, InvalidSpeedException {
        if (stalled) {
            throw new MotorStallException(id, name + " is stalled, cannot actuate");
        }
        setSpeed(command.getSpeed());
        direction = command.getDirection();
        System.out.println(name + " moving " + direction + " at speed " + speed);
    }

    public void stop() {
        speed = 0;
        direction = Direction.STOP;
        System.out.println(name + " STOPPED");
    }

    public void setStalled(boolean stalled) {
        this.stalled = stalled;
    }

    public boolean isStalled() {
        return stalled;
    }

    public int getSpeed() {
        return speed;
    }

    public Direction getDirection() {
        return direction;
    }

    @Override
    public String getDescription() {
        return "Drives one wheel of the chassis";
    }
}
