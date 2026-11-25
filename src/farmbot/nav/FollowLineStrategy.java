package farmbot.nav;

import farmbot.data.Reading;
import farmbot.exception.LineLostException;
import farmbot.model.Direction;
import farmbot.model.SensorType;
import farmbot.sensor.LineSensor;

import java.util.Map;

/**
 * Classic three-eye line follower.
 *   0 1 0 / 1 1 1 -> FORWARD
 *   1 0 0 / 1 1 0 -> LEFT  (line has moved under the left eye)
 *   0 0 1 / 0 1 1 -> RIGHT
 *   0 0 0         -> LineLostException (the bot switches to SearchStrategy)
 *
 * Stateless, so one instance can be reused safely.
 */
public class FollowLineStrategy implements NavigationStrategy {

    public static final String NAME = "FOLLOW_LINE";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Direction decide(Map<SensorType, Reading> latest) throws LineLostException {
        Reading line = latest.get(SensorType.LINE);
        if (line == null) {
            return Direction.STOP; // no data yet: don't move blind
        }
        boolean[] eyes = LineSensor.eyes(line);
        boolean left = eyes[0];
        boolean centre = eyes[1];
        boolean right = eyes[2];

        if (!left && !centre && !right) {
            throw new LineLostException("All three eyes see no line (0 0 0)");
        }
        if (left && !right) {
            return Direction.LEFT;
        }
        if (right && !left) {
            return Direction.RIGHT;
        }
        return Direction.FORWARD;
    }
}
