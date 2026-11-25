package farmbot.data;

import farmbot.model.ReadingStatus;
import farmbot.model.SensorType;
import farmbot.model.StatusLabel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * One measurement from one sensor or module, matching one row of sensor_readings.
 *
 * IMMUTABLE: the class is final, every field is private final, and there are no
 * setters. Once created, a Reading can never change, so the poller threads,
 * the DbWriter thread and the web thread can all share the same object
 * without locks.
 */
public final class Reading {

    /** Timestamp format used in the CSV seed files and the database. */
    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String sensorId;
    private final SensorType sensorType;
    private final double value;
    private final String unit;
    private final String raw;
    private final StatusLabel label;
    private final LocalDateTime recordedAt;

    public Reading(String sensorId, SensorType sensorType, double value, String unit,
                   String raw, StatusLabel label, LocalDateTime recordedAt) {
        this.sensorId = Objects.requireNonNull(sensorId, "sensorId");
        this.sensorType = Objects.requireNonNull(sensorType, "sensorType");
        this.value = value;
        this.unit = unit == null ? "" : unit;
        this.raw = raw == null ? "" : raw;
        this.label = Objects.requireNonNull(label, "label");
        this.recordedAt = Objects.requireNonNull(recordedAt, "recordedAt");
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

    public String getUnit() {
        return unit;
    }

    public String getRaw() {
        return raw;
    }

    /** The exact status word, e.g. LINE_LOST. Stored in the status column. */
    public StatusLabel getLabel() {
        return label;
    }

    /** The generic severity level of the word, e.g. WARNING. Used for colours and rules. */
    public ReadingStatus getStatus() {
        return label.getLevel();
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public String getRecordedAtText() {
        return recordedAt.format(TIME_FORMAT);
    }

    @Override
    public String toString() {
        return String.format("%s %s=%.2f%s [%s/%s] raw='%s' @ %s",
                sensorId, sensorType, value, unit, label, label.getLevel(), raw, getRecordedAtText());
    }
}
