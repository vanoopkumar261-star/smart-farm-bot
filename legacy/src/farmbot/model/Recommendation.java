package farmbot.model;

public class Recommendation {
    private final String message;
    private final long timestamp;

    public Recommendation(String message) {
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
