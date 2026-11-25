package farmbot.exception;

/**
 * Thrown by Motor.setSpeed for a value outside 0-100.
 * Unchecked (extends IllegalArgumentException) because passing a bad speed is a
 * programming error, the same family as passing a null to a constructor.
 */
public class InvalidSpeedException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    private final int requestedSpeed;

    public InvalidSpeedException(int requestedSpeed) {
        super("Speed must be between 0 and 100, got " + requestedSpeed);
        this.requestedSpeed = requestedSpeed;
    }

    public int getRequestedSpeed() {
        return requestedSpeed;
    }
}
