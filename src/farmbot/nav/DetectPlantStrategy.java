package farmbot.nav;

import farmbot.data.Reading;
import farmbot.exception.LineLostException;
import farmbot.model.Direction;
import farmbot.model.SensorType;
import farmbot.model.StatusLabel;

import java.util.Map;

/**
 * Follows the crop row and pauses at each plant so the mission modules can
 * sample it.
 *
 * A plant is "something 20-50 cm ahead" (obstacle sensor = NEAR); anything
 * closer is a HALT, which the bot handles above every strategy. This is the
 * simulation's assumption, since the camera is inactive.
 *
 * REUSE BY COMPOSITION: driving between plants is exactly line-following, so
 * this class HAS a FollowLineStrategy and delegates to it, instead of copying
 * its code.
 */
public class DetectPlantStrategy implements NavigationStrategy {

    public static final String NAME = "DETECT_PLANT";

    private final FollowLineStrategy follow = new FollowLineStrategy();
    private final int dwellTicks;

    private int dwellRemaining;
    private boolean currentPlantInspected;
    private int plantsInspected;

    public DetectPlantStrategy() {
        this(3);
    }

    public DetectPlantStrategy(int dwellTicks) {
        if (dwellTicks < 1) {
            throw new IllegalArgumentException("dwellTicks must be at least 1");
        }
        this.dwellTicks = dwellTicks;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public synchronized void reset() {
        dwellRemaining = 0;
        currentPlantInspected = false;
    }

    @Override
    public synchronized Direction decide(Map<SensorType, Reading> latest) throws LineLostException {
        Reading obstacle = latest.get(SensorType.OBSTACLE);
        boolean plantAhead = obstacle != null && obstacle.getLabel() == StatusLabel.NEAR;

        if (plantAhead && !currentPlantInspected) {
            currentPlantInspected = true; // inspect each plant once, not on every tick
            dwellRemaining = dwellTicks;
            plantsInspected++;
        } else if (!plantAhead) {
            currentPlantInspected = false; // passed it; ready for the next one
        }

        if (dwellRemaining > 0) {
            dwellRemaining--;
            return Direction.STOP;
        }
        return follow.decide(latest); // may throw LineLostException, handled by the bot as usual
    }

    public synchronized int getPlantsInspected() {
        return plantsInspected;
    }
}
