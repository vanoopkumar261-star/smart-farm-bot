package farmbot.sensor;

import farmbot.data.Reading;
import farmbot.model.SensorType;
import farmbot.model.StatusLabel;

/**
 * Three-eye infrared line sensor (Left, Centre, Right).
 *
 * The value is the three eyes packed into one number: L*4 + C*2 + R.
 *   2 = 0 1 0 (line under the centre)   4 = 1 0 0 (line to the left)
 *   1 = 0 0 1 (line to the right)       0 = 0 0 0 (line lost)
 * The raw column holds the readable form, e.g. "0 1 0".
 *
 * Critical: the rover cannot navigate without it.
 */
public class LineSensor extends Sensor {

    public LineSensor(String id, SignalSource source) {
        super(id, "IR Line Array (3-eye)", SensorType.LINE, true, source);
    }

    @Override
    protected StatusLabel classify(double value) {
        boolean[] eyes = decode(toPattern(value));
        if (!eyes[0] && !eyes[1] && !eyes[2]) {
            return StatusLabel.LINE_LOST;
        }
        boolean centredOnly = eyes[1] && !eyes[0] && !eyes[2];
        return centredOnly ? StatusLabel.ON_LINE : StatusLabel.DRIFT;
    }

    @Override
    protected String formatRaw(double value) {
        boolean[] eyes = decode(toPattern(value));
        return bit(eyes[0]) + " " + bit(eyes[1]) + " " + bit(eyes[2]);
    }

    /** Unpacks a line Reading into {left, centre, right}. Used by the navigation strategies. */
    public static boolean[] eyes(Reading reading) {
        return decode(toPattern(reading.getValue()));
    }

    private static int toPattern(double value) {
        return (int) Math.round(value) & 0b111;
    }

    private static boolean[] decode(int pattern) {
        return new boolean[] {(pattern & 0b100) != 0, (pattern & 0b010) != 0, (pattern & 0b001) != 0};
    }

    private static String bit(boolean on) {
        return on ? "1" : "0";
    }
}
