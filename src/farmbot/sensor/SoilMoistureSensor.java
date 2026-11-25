package farmbot.sensor;

import farmbot.model.SensorType;
import farmbot.model.StatusLabel;
import farmbot.model.Thresholds;

/**
 * Capacitive soil-moisture probe, value in % volumetric water content.
 * Raw = a 10-bit ADC count; a capacitive probe reads LOWER when the soil is wetter.
 */
public class SoilMoistureSensor extends Sensor {

    private static final int ADC_DRY = 850;
    private static final int ADC_PER_PERCENT = 5;

    public SoilMoistureSensor(String id, SignalSource source) {
        super(id, "Capacitive Soil Moisture Probe", SensorType.SOIL_MOISTURE, false, source);
    }

    @Override
    protected StatusLabel classify(double value) {
        if (value < Thresholds.MOISTURE_CRITICAL) {
            return StatusLabel.VERY_DRY;
        }
        if (value < Thresholds.MOISTURE_LOW) {
            return StatusLabel.DRY;
        }
        return StatusLabel.NORMAL;
    }

    @Override
    protected String formatRaw(double value) {
        return "ADC=" + Math.round(ADC_DRY - value * ADC_PER_PERCENT);
    }
}
