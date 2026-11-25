package farmbot.exceptions;

public class SensorFailureException extends Exception {
    private final String sensorId;
    private final boolean critical;

    public SensorFailureException(String sensorId, String message, boolean critical) {
        super(message);
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
