package farmbot.nav;

import farmbot.data.Reading;
import farmbot.model.Direction;
import farmbot.model.SensorType;

import java.util.Map;

/**
 * STUB: manual driving needs a joystick or remote link, which the project does
 * not have yet. decide() throws UnsupportedOperationException. The bot catches
 * it, logs a fault, cuts the motors and keeps running; the API returns a clean
 * 400 error instead of crashing.
 */
public class ManualStrategy implements NavigationStrategy {

    public static final String NAME = "MANUAL";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Direction decide(Map<SensorType, Reading> latest) {
        throw new UnsupportedOperationException("Manual control needs a remote controller, which is not implemented yet");
    }
}
