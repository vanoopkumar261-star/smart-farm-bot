package farmbot.navigation;

import farmbot.model.Command;
import farmbot.model.Direction;
import farmbot.model.SensorReading;

import java.util.List;

/** Small internal state machine: turn -> cross -> turn -> align, then repeats. */
public class LaneChangeStrategy implements NavigationStrategy {
    private int stage = 0;

    @Override
    public Command decide(List<SensorReading> readings) {
        Command command;
        switch (stage) {
            case 0:
                command = new Command(Direction.RIGHT, 50);
                break;
            case 1:
                command = new Command(Direction.FORWARD, 50);
                break;
            case 2:
                command = new Command(Direction.LEFT, 50);
                break;
            default:
                command = new Command(Direction.FORWARD, 50);
                stage = -1;
                break;
        }
        stage++;
        return command;
    }

    @Override
    public String getName() {
        return "LANE_CHANGE";
    }
}
