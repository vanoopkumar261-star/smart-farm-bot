package farmbot.navigation;

import farmbot.model.Command;
import farmbot.model.SensorReading;

import java.util.List;

/** Deliberate stub - manual control isn't implemented yet (future work). */
public class ManualStrategy implements NavigationStrategy {
    @Override
    public Command decide(List<SensorReading> readings) {
        throw new UnsupportedOperationException("manual control not yet implemented");
    }

    @Override
    public String getName() {
        return "MANUAL";
    }
}
