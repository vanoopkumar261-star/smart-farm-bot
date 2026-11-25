package farmbot.hardware;

import farmbot.exceptions.InvalidSpeedException;
import farmbot.exceptions.MotorStallException;
import farmbot.model.Command;
import farmbot.model.Direction;

public class DriveTrain {
    private final Motor leftMotor;
    private final Motor rightMotor;

    public DriveTrain() {
        leftMotor = new Motor("motor-L", "Left Motor");
        rightMotor = new Motor("motor-R", "Right Motor");
    }

    public void execute(Command command) throws MotorStallException, InvalidSpeedException {
        switch (command.getDirection()) {
            case FORWARD:
                leftMotor.actuate(new Command(Direction.FORWARD, command.getSpeed()));
                rightMotor.actuate(new Command(Direction.FORWARD, command.getSpeed()));
                break;
            case LEFT:
                leftMotor.actuate(new Command(Direction.BACKWARD, command.getSpeed()));
                rightMotor.actuate(new Command(Direction.FORWARD, command.getSpeed()));
                break;
            case RIGHT:
                leftMotor.actuate(new Command(Direction.FORWARD, command.getSpeed()));
                rightMotor.actuate(new Command(Direction.BACKWARD, command.getSpeed()));
                break;
            case BACKWARD:
                leftMotor.actuate(new Command(Direction.BACKWARD, command.getSpeed()));
                rightMotor.actuate(new Command(Direction.BACKWARD, command.getSpeed()));
                break;
            case STOP:
            default:
                stop();
        }
    }

    public void stop() {
        leftMotor.stop();
        rightMotor.stop();
    }

    public void cutPower() {
        stop();
    }

    public Motor getLeftMotor() {
        return leftMotor;
    }

    public Motor getRightMotor() {
        return rightMotor;
    }
}
