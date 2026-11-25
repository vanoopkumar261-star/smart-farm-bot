package farmbot.data;

import java.time.LocalDateTime;
import java.util.Objects;

/** One row of fault_log: a caught problem, kept for the Faults panel. Immutable. */
public final class FaultRecord {

    private final LocalDateTime faultTime;
    private final String source;
    private final String exception;
    private final String message;

    public FaultRecord(LocalDateTime faultTime, String source, String exception, String message) {
        this.faultTime = Objects.requireNonNull(faultTime, "faultTime");
        this.source = Objects.requireNonNull(source, "source");
        this.exception = Objects.requireNonNull(exception, "exception");
        this.message = message == null ? "" : message;
    }

    public LocalDateTime getFaultTime() {
        return faultTime;
    }

    public String getFaultTimeText() {
        return faultTime.format(Reading.TIME_FORMAT);
    }

    public String getSource() {
        return source;
    }

    public String getException() {
        return exception;
    }

    public String getMessage() {
        return message;
    }
}
