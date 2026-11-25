package farmbot.nav;

import farmbot.data.Reading;
import farmbot.exception.LineLostException;
import farmbot.model.Direction;
import farmbot.model.SensorType;

import java.util.Map;

/**
 * STRATEGY PATTERN: each way of driving is its own class behind this interface.
 * SmartFarmBot holds one NavigationStrategy and calls decide() on every tick; it
 * never knows which concrete strategy it has. Swapping behaviour at runtime is
 * just bot.setStrategy(new SomethingElse()).
 *
 * Safety (stopping for obstacles) is NOT done here. The bot checks it above
 * whichever strategy is active, so no strategy can forget to stop.
 */
public interface NavigationStrategy {

    /** Name shown in the UI and accepted by POST /api/strategy. */
    String getName();

    /**
     * Choose the next steering direction from the latest sensor readings.
     *
     * @throws LineLostException if the strategy needs the line and cannot see it
     */
    Direction decide(Map<SensorType, Reading> latest) throws LineLostException;

    /** Called when this strategy becomes active, so it can clear old state. */
    default void reset() {
        // stateless strategies have nothing to reset
    }

    /**
     * A temporary strategy (search, lane change) returns true when its job is
     * done; the bot then goes back to the strategy it was using before.
     */
    default boolean isFinished(Map<SensorType, Reading> latest) {
        return false;
    }
}
