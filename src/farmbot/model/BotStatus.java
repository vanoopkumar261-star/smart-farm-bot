package farmbot.model;

/** Overall state of the rover. Read by the web thread, written by the control thread. */
public enum BotStatus {
    IDLE("Idle, motors off"),
    RUNNING("Following the line"),
    SEARCHING("Line lost, searching"),
    HALTED("Halted for an obstacle"),
    FAULT("Stopped after a critical fault"),
    SHUTDOWN("Shut down");

    private final String description;

    BotStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /** True if the drive motors are allowed to receive power in this state. */
    public boolean canDrive() {
        return this == RUNNING || this == SEARCHING;
    }
}
