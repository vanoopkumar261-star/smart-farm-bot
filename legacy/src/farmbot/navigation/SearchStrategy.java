package farmbot.navigation;

import farmbot.exceptions.LineLostException;
import farmbot.model.Command;
import farmbot.model.Direction;
import farmbot.model.SensorReading;

import java.util.List;

public class SearchStrategy implements NavigationStrategy {
    private static final int MAX_TICKS = 20;
    private int ticksSearching = 0;

    @Override
    public Command decide(List<SensorReading> readings) throws LineLostException {
        ticksSearching++;
        if (ticksSearching > MAX_TICKS) {
            throw new LineLostException("search timeout - line not reacquired", 0, 0);
        }
        return new Command(Direction.LEFT, 20);
    }

    @Override
    public String getName() {
        return "SEARCH";
    }
}
