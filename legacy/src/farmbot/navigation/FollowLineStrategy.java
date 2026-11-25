package farmbot.navigation;

import farmbot.exceptions.LineLostException;
import farmbot.model.Command;
import farmbot.model.Direction;
import farmbot.model.SensorReading;
import farmbot.model.SensorType;

import java.util.List;

public class FollowLineStrategy implements NavigationStrategy {
    @Override
    public Command decide(List<SensorReading> readings) throws LineLostException {
        SensorReading line = Readings.find(readings, SensorType.LINE);
        if (line == null) {
            throw new LineLostException("no line sensor reading available", 0, 0);
        }
        int encoded = (int) line.getValue();
        int left = (encoded / 100) % 10;
        int center = (encoded / 10) % 10;
        int right = encoded % 10;

        if (left == 0 && center == 0 && right == 0) {
            throw new LineLostException("line not detected", 0, 0);
        }
        if (left == 0 && center == 1 && right == 0) {
            return new Command(Direction.FORWARD, 100);
        }
        if (left == 1 && center == 0 && right == 0) {
            return new Command(Direction.LEFT, 80);
        }
        if (left == 0 && center == 0 && right == 1) {
            return new Command(Direction.RIGHT, 80);
        }
        if (left == 1 && center == 1 && right == 1) {
            return new Command(Direction.FORWARD, 100);
        }
        return new Command(Direction.FORWARD, 70);
    }

    @Override
    public String getName() {
        return "FOLLOW_LINE";
    }
}
