package farmbot.nav;

import farmbot.data.Reading;
import farmbot.model.Direction;
import farmbot.model.SensorType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Moves the rover across to the next crop row with a fixed manoeuvre:
 *   turn 90 degrees (2 ticks), cross the gap (3 ticks), turn back (2 ticks).
 * When the plan is used up, isFinished() is true and the bot returns to
 * the strategy it was using before (normally line-following).
 */
public class LaneChangeStrategy implements NavigationStrategy {

    public static final String NAME = "LANE_CHANGE";

    private static final int TURN_TICKS = 2;
    private static final int CROSS_TICKS = 3;

    private final List<Direction> plan;
    private int step;

    /** @param toRight true to move to the row on the right, false for the left */
    public LaneChangeStrategy(boolean toRight) {
        Direction turnOut = toRight ? Direction.SPIN_RIGHT : Direction.SPIN_LEFT;
        Direction turnBack = toRight ? Direction.SPIN_LEFT : Direction.SPIN_RIGHT;
        List<Direction> steps = new ArrayList<>();
        steps.addAll(Collections.nCopies(TURN_TICKS, turnOut));
        steps.addAll(Collections.nCopies(CROSS_TICKS, Direction.FORWARD));
        steps.addAll(Collections.nCopies(TURN_TICKS, turnBack));
        this.plan = List.copyOf(steps); // unmodifiable
    }

    public LaneChangeStrategy() {
        this(true);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public synchronized void reset() {
        step = 0;
    }

    @Override
    public synchronized Direction decide(Map<SensorType, Reading> latest) {
        if (step >= plan.size()) {
            return Direction.STOP;
        }
        return plan.get(step++);
    }

    @Override
    public synchronized boolean isFinished(Map<SensorType, Reading> latest) {
        return step >= plan.size();
    }

    public List<Direction> getPlan() {
        return plan;
    }
}
