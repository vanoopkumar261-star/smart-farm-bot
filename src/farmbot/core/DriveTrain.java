package farmbot.core;

import farmbot.exception.InvalidSpeedException;
import farmbot.model.Direction;

import java.util.Objects;

/**
 * Differential drive: a left and a right Motor. Steering works by running the
 * two sides at different speeds.
 *
 * COMPOSITION: DriveTrain HAS two Motors. It does not extend Motor, because a
 * drive train is not a kind of motor.
 *
 * apply(direction) needs no if-ladder: each Direction already knows its
 * left/right power split.
 */
public class DriveTrain {

    private final Motor left;
    private final Motor right;
    private volatile int cruiseSpeed;
    private volatile Direction currentDirection = Direction.STOP;

    public DriveTrain(Motor left, Motor right, int cruiseSpeed) {
        this.left = Objects.requireNonNull(left, "left");
        this.right = Objects.requireNonNull(right, "right");
        // A private helper, not setCruiseSpeed(): a constructor must not call a
        // method that a subclass could override before the object is fully built.
        this.cruiseSpeed = validSpeed(cruiseSpeed);
    }

    public void setCruiseSpeed(int cruiseSpeed) {
        this.cruiseSpeed = validSpeed(cruiseSpeed);
    }

    private static int validSpeed(int speed) {
        if (speed < Motor.MIN_SPEED || speed > Motor.MAX_SPEED) {
            throw new InvalidSpeedException(speed);
        }
        return speed;
    }

    public void apply(Direction direction) {
        Objects.requireNonNull(direction, "direction");
        drive(left, direction.getLeftFactor());
        drive(right, direction.getRightFactor());
        currentDirection = direction;
    }

    private void drive(Motor motor, double factor) {
        motor.setReversed(factor < 0);
        motor.setSpeed((int) Math.round(Math.abs(factor) * cruiseSpeed));
    }

    /** Cut both motors. Called from finally blocks, so it must never throw. */
    public void cutPower() {
        left.cutPower();
        right.cutPower();
        currentDirection = Direction.STOP;
    }

    /** Average of the two motors, 0-100. */
    public int getCurrentSpeed() {
        return (left.getSpeed() + right.getSpeed()) / 2;
    }

    public int getCruiseSpeed() {
        return cruiseSpeed;
    }

    public Direction getCurrentDirection() {
        return currentDirection;
    }

    public Motor getLeft() {
        return left;
    }

    public Motor getRight() {
        return right;
    }
}
