package farmbot.exceptions;

public class MotorStallException extends Exception {
    private final String motorId;

    public MotorStallException(String motorId, String message) {
        super(message);
        this.motorId = motorId;
    }

    public String getMotorId() {
        return motorId;
    }
}
