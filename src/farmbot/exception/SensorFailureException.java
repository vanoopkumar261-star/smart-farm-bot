package farmbot.exception;

/**
 * A sensor could not produce a reading.
 * Critical failures (e.g. the line sensor) halt the bot and go to fault_log.
 * Non-critical ones (e.g. the camera) only change that sensor's UI status.
 */
public class SensorFailureException extends FarmBotException {

    private static final long serialVersionUID = 1L;

    private final String sensorId;
    private final boolean critical;

    public SensorFailureException(String sensorId, boolean critical, String message) {
        super(message);
        this.sensorId = sensorId;
        this.critical = critical;
    }

    public SensorFailureException(String sensorId, boolean critical, String message, Throwable cause) {
        super(message, cause);
        this.sensorId = sensorId;
        this.critical = critical;
    }

    public String getSensorId() {
        return sensorId;
    }

    public boolean isCritical() {
        return critical;
    }
}
