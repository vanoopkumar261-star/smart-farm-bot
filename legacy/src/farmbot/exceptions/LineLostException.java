package farmbot.exceptions;

public class LineLostException extends Exception {
    private final int lastKnownX;
    private final int lastKnownY;

    public LineLostException(String message, int lastKnownX, int lastKnownY) {
        super(message);
        this.lastKnownX = lastKnownX;
        this.lastKnownY = lastKnownY;
    }

    public int getLastKnownX() {
        return lastKnownX;
    }

    public int getLastKnownY() {
        return lastKnownY;
    }
}
