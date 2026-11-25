package farmbot.model;

public class Fault {
    private final String sensorId;
    private final String message;
    private final long timestamp;
    private final boolean critical;

    public Fault(String sensorId, String message, boolean critical) {
        this.sensorId = sensorId;
        this.message = message;
        this.critical = critical;
        this.timestamp = System.currentTimeMillis();
    }

    public String getSensorId() {
        return sensorId;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isCritical() {
        return critical;
    }
}
