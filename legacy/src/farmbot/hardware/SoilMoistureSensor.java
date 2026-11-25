package farmbot.hardware;

import farmbot.model.SensorReading;
import farmbot.model.SensorType;

public class SoilMoistureSensor extends Sensor {
    private int moistureLevel;

    public SoilMoistureSensor(String id) {
        super(id, "Soil Moisture Sensor", SensorType.SOIL_MOISTURE);
        moistureLevel = 0;
    }

    public void setMoistureLevel(int moistureLevel) {
        this.moistureLevel = moistureLevel;
    }

    public int getMoistureLevel() {
        return moistureLevel;
    }

    @Override
    public SensorReading read() {
        SensorReading reading = new SensorReading(id, type, moistureLevel, moistureLevel + "%");
        logReading(reading);
        return reading;
    }

    @Override
    public String getDescription() {
        return "Reads soil moisture percentage";
    }
}
