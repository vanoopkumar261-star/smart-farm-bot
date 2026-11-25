package farmbot.model;

public class ObstacleEvent {
    private final long timestamp;
    private final boolean detected;

    public ObstacleEvent(boolean detected) {
        this.timestamp = System.currentTimeMillis();
        this.detected = detected;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isDetected() {
        return detected;
    }
}
