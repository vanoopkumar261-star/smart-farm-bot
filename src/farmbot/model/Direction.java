package farmbot.model;

/**
 * A steering decision produced by a NavigationStrategy. Each constant knows the
 * power split for the left and right motors (as a fraction of the cruise speed),
 * so DriveTrain can apply any direction without an if-ladder.
 */
public enum Direction {
    FORWARD(1.0, 1.0),
    LEFT(0.3, 1.0),
    RIGHT(1.0, 0.3),
    SPIN_LEFT(-0.5, 0.5),
    SPIN_RIGHT(0.5, -0.5),
    STOP(0.0, 0.0);

    private final double leftFactor;
    private final double rightFactor;

    Direction(double leftFactor, double rightFactor) {
        this.leftFactor = leftFactor;
        this.rightFactor = rightFactor;
    }

    public double getLeftFactor() {
        return leftFactor;
    }

    public double getRightFactor() {
        return rightFactor;
    }
}
