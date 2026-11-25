package farmbot.model;

public class SensorReading {
    private final String sensorId;
    private final SensorType sensorType;
    private final double value;
    private final long timestamp;
    private final String display;
    private boolean flagObstacle;

    public SensorReading(String sensorId, SensorType sensorType, double value, String display) {
        this.sensorId = sensorId;
        this.sensorType = sensorType;
        this.value = value;
        this.display = display;
        this.timestamp = System.currentTimeMillis();
    }

    public String getSensorId() {
        return sensorId;
    }

    public SensorType getSensorType() {
        return sensorType;
    }

    public double getValue() {
        return value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getDisplay() {
        return display;
    }

    public boolean isFlagObstacle() {
        return flagObstacle;
    }

    public void setFlagObstacle(boolean flagObstacle) {
        this.flagObstacle = flagObstacle;
    }
}
