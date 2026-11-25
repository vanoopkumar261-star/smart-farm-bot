package farmbot.data;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * One row of obstacle_events. The distance is optional: an undetected collision
 * (something that passed under the sonar cone) has no distance reading.
 */
public final class ObstacleEvent {

    private final LocalDateTime eventTime;
    private final Double distanceCm; // null = not measured (undetected collision)
    private final String outcome;    // DETECTED_AVOIDED, HALTED, UNDETECTED_COLLISION

    public ObstacleEvent(LocalDateTime eventTime, Double distanceCm, String outcome) {
        this.eventTime = Objects.requireNonNull(eventTime, "eventTime");
        this.distanceCm = distanceCm;
        this.outcome = Objects.requireNonNull(outcome, "outcome");
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public String getEventTimeText() {
        return eventTime.format(Reading.TIME_FORMAT);
    }

    /** May be null for an undetected collision. */
    public Double getDistanceCm() {
        return distanceCm;
    }

    public String getOutcome() {
        return outcome;
    }

    /** True unless this was a collision that the sonar never saw. */
    public boolean wasDetected() {
        return !"UNDETECTED_COLLISION".equals(outcome);
    }
}
