package farmbot.sensor;

import farmbot.model.SensorType;
import farmbot.model.StatusLabel;
import farmbot.model.Thresholds;

/**
 * LM35-style soil temperature probe, value in degrees C.
 * Raw = the probe's output voltage; an LM35 gives 10 mV per degree.
 */
public class SoilTemperatureSensor extends Sensor {

    private static final double MILLIVOLTS_PER_DEGREE = 10.0;

    public SoilTemperatureSensor(String id, SignalSource source) {
        super(id, "Soil Temperature Probe", SensorType.SOIL_TEMPERATURE, false, source);
    }

    @Override
    protected StatusLabel classify(double value) {
        if (value < Thresholds.SOIL_TEMP_MIN) {
            return StatusLabel.COLD;
        }
        if (value > Thresholds.SOIL_TEMP_MAX) {
            return StatusLabel.HOT;
        }
        return StatusLabel.NORMAL;
    }

    @Override
    protected String formatRaw(double value) {
        return String.format("%.0fmV", value * MILLIVOLTS_PER_DEGREE);
    }
}
