package farmbot.hardware;

import farmbot.exceptions.SensorFailureException;
import farmbot.model.SensorReading;
import farmbot.model.SensorType;

import java.net.ConnectException;

public class Esp32CamSensor extends Sensor {
    private final boolean connected;

    public Esp32CamSensor(String id) {
        super(id, "ESP32-CAM", SensorType.CAMERA);
        this.connected = false;
        this.status = "INACTIVE";
    }

    @Override
    public SensorReading read() throws SensorFailureException {
        try {
            if (!connected) {
                throw new ConnectException("ESP32-CAM not connected: no hardware link");
            }
            // Unreachable in simulation - no physical camera exists yet.
            SensorReading reading = new SensorReading(id, type, 0, "frame received");
            logReading(reading);
            return reading;
        } catch (ConnectException e) {
            status = "INACTIVE";
            throw new SensorFailureException(
                id,
                "ESP32-CAM inactive (simulated - no hardware attached)",
                false
            );
        }
    }

    @Override
    public String getDescription() {
        return "Live field video feed via ESP32-CAM module (simulated - inactive, no hardware attached)";
    }
}
