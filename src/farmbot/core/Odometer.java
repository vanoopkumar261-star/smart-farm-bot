package farmbot.core;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Distance travelled. At 100% speed the rover covers 0.5 m/s.
 *
 * The total is stored as whole millimetres in an AtomicLong, so the control
 * thread can add to it while the web thread reads it, without a lock and
 * without losing updates.
 */
public class Odometer {

    private static final double METRES_PER_SECOND_AT_FULL_SPEED = 0.5;

    private final AtomicLong millimetres = new AtomicLong();

    /** Record travel at the given speed (0-100) for the given number of seconds. */
    public void record(int speedPercent, double seconds) {
        if (speedPercent <= 0 || seconds <= 0) {
            return;
        }
        double metres = METRES_PER_SECOND_AT_FULL_SPEED * (speedPercent / 100.0) * seconds;
        millimetres.addAndGet(Math.round(metres * 1000));
    }

    public double getDistanceMetres() {
        return millimetres.get() / 1000.0;
    }

    public void reset() {
        millimetres.set(0);
    }
}
