package farmbot.sensor;

import farmbot.model.SensorType;
import farmbot.model.StatusLabel;
import farmbot.model.Thresholds;

/**
 * Battery state of charge, value in %.
 * Raw = pack voltage for a 3-cell pack (about 10.5 V empty, 12.7 V full).
 * Critical: a flat battery must stop the rover safely.
 */
public class BatterySensor extends Sensor {

    private static final double VOLTS_EMPTY = 10.5;
    private static final double VOLTS_RANGE = 2.2;

    public BatterySensor(String id, SignalSource source) {
        super(id, "Battery Monitor (3S pack)", SensorType.BATTERY, true, source);
    }

    @Override
    protected StatusLabel classify(double value) {
        if (value < Thresholds.BATTERY_CRITICAL) {
            return StatusLabel.EMPTY;
        }
        if (value < Thresholds.BATTERY_LOW) {
            return StatusLabel.LOW;
        }
        return StatusLabel.NORMAL;
    }

    @Override
    protected String formatRaw(double value) {
        return String.format("%.2fV", VOLTS_EMPTY + value / 100.0 * VOLTS_RANGE);
    }
}
