package farmbot.rules;

import farmbot.data.Reading;
import farmbot.model.SensorType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Everything a Rule may look at, gathered into one read-only object: the latest
 * reading of each sensor type, and the number of obstacle halts so far.
 *
 * Passing one context object (instead of the whole bot) keeps the rules
 * decoupled and very easy to unit-test.
 */
public final class RuleContext {

    private final Map<SensorType, Reading> latest;
    private final int obstacleEventCount;

    public RuleContext(Map<SensorType, Reading> latest, int obstacleEventCount) {
        if (obstacleEventCount < 0) {
            throw new IllegalArgumentException("obstacleEventCount cannot be negative");
        }
        // Defensive copy: later changes to the caller's map cannot affect this context.
        this.latest = latest.isEmpty() ? new EnumMap<>(SensorType.class) : new EnumMap<>(latest);
        this.obstacleEventCount = obstacleEventCount;
    }

    public Optional<Reading> latest(SensorType type) {
        return Optional.ofNullable(latest.get(type));
    }

    public int getObstacleEventCount() {
        return obstacleEventCount;
    }
}
