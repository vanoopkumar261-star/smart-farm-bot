package farmbot.navigation;

import farmbot.exceptions.LineLostException;
import farmbot.model.Command;
import farmbot.model.SensorReading;

import java.util.List;

public interface NavigationStrategy {
    Command decide(List<SensorReading> readings) throws LineLostException;

    String getName();
}
