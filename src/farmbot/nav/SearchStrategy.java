package farmbot.nav;

import farmbot.data.Reading;
import farmbot.model.Direction;
import farmbot.model.SensorType;
import farmbot.sensor.LineSensor;

import java.util.Map;

/**
 * Recovery strategy used after a LineLostException.
 *
 * Spins on the spot in a widening sweep: 2 ticks left, 4 right, 8 left, 16 right
 * (capped at 16). Each swing crosses back over the start and goes further, so the
 * line is found even if it is some way off. As soon as any eye sees the line,
 * isFinished() returns true and the bot goes back to following it.
 *
 * Synchronized because the web thread may reset() it while the control thread
 * is calling decide().
 */
public class SearchStrategy implements NavigationStrategy {

    public static final String NAME = "SEARCH";

    private static final int FIRST_SWEEP = 2;
    private static final int MAX_SWEEP = 16;

    private int sweepLength = FIRST_SWEEP;
    private int ticksInSweep;
    private boolean sweepingLeft = true;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public synchronized void reset() {
        sweepLength = FIRST_SWEEP;
        ticksInSweep = 0;
        sweepingLeft = true;
    }

    @Override
    public synchronized Direction decide(Map<SensorType, Reading> latest) {
        if (lineVisible(latest)) {
            return Direction.FORWARD;
        }
        if (ticksInSweep >= sweepLength) {
            sweepingLeft = !sweepingLeft;
            sweepLength = Math.min(sweepLength * 2, MAX_SWEEP);
            ticksInSweep = 0;
        }
        ticksInSweep++;
        return sweepingLeft ? Direction.SPIN_LEFT : Direction.SPIN_RIGHT;
    }

    @Override
    public boolean isFinished(Map<SensorType, Reading> latest) {
        return lineVisible(latest);
    }

    private static boolean lineVisible(Map<SensorType, Reading> latest) {
        Reading line = latest.get(SensorType.LINE);
        if (line == null) {
            return false;
        }
        boolean[] eyes = LineSensor.eyes(line);
        return eyes[0] || eyes[1] || eyes[2];
    }
}
