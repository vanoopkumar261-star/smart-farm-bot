package farmbot.sensor;

import farmbot.model.SensorType;
import farmbot.model.StatusLabel;
import farmbot.model.Thresholds;

/**
 * HC-SR04-style ultrasonic distance sensor, value in cm.
 * Raw = the echo pulse length; sound takes about 58 microseconds per cm there and back.
 * Critical: the rover must not drive blind.
 */
public class ObstacleSensor extends Sensor {

    private static final int MICROSECONDS_PER_CM = 58;

    public ObstacleSensor(String id, SignalSource source) {
        super(id, "Ultrasonic Range Finder", SensorType.OBSTACLE, true, source);
    }

    @Override
    protected StatusLabel classify(double value) {
        if (value < Thresholds.OBSTACLE_HALT_CM) {
            return StatusLabel.HALT;
        }
        if (value < Thresholds.OBSTACLE_WARN_CM) {
            return StatusLabel.NEAR;
        }
        return StatusLabel.CLEAR;
    }

    @Override
    protected String formatRaw(double value) {
        return "echo=" + Math.round(value * MICROSECONDS_PER_CM) + "us";
    }
}
