package farmbot.model;

/**
 * Every cut-off value lives here. Sensors use these to set a Reading's status,
 * the rules use them to raise advisories, and the sprayer uses them to switch
 * on. Keeping them in one place means the three can never disagree.
 *
 * final class + private constructor = a constants holder that cannot be
 * instantiated or extended.
 */
public final class Thresholds {

    public static final double MOISTURE_LOW = 30.0;          // % - irrigation advisory, sprayer on
    public static final double MOISTURE_CRITICAL = 15.0;     // %
    public static final double MOISTURE_SPRAY_STOP = 35.0;   // % - sprayer off again (hysteresis)

    public static final double OBSTACLE_HALT_CM = 20.0;      // closer than this = stop
    public static final double OBSTACLE_WARN_CM = 50.0;

    public static final double BATTERY_LOW = 20.0;           // %
    public static final double BATTERY_CRITICAL = 10.0;      // %

    public static final double SOIL_TEMP_MIN = 5.0;          // C
    public static final double SOIL_TEMP_MAX = 35.0;         // C

    private Thresholds() {
        // constants only
    }
}
