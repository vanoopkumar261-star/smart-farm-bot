package farmbot.sensor;

import farmbot.data.Reading;
import farmbot.exception.SensorFailureException;
import farmbot.model.SensorType;
import farmbot.model.StatusLabel;

import java.io.IOException;

/**
 * ESP32-CAM camera. It is fitted in the design but INACTIVE: there is no camera
 * stream in the simulation.
 *
 * This class overrides read() completely instead of using the template. It shows
 * EXCEPTION TRANSLATION: the low-level IOException from the connection attempt is
 * caught and re-thrown as a NON-critical SensorFailureException. The bot catches
 * that and shows "INACTIVE" in the UI; nothing crashes.
 */
public class CameraSensor extends Sensor {

    private static final String STREAM_URL = "http://esp32-cam.local/stream";

    public CameraSensor(String id) {
        // The camera never produces a number, so it gets a lambda that always returns 0.
        super(id, "ESP32-CAM", SensorType.CAMERA, false, () -> 0.0);
    }

    @Override
    public Reading read() throws SensorFailureException {
        try {
            connect();
        } catch (IOException e) {
            throw new SensorFailureException(getId(), false, "Camera INACTIVE: " + e.getMessage(), e);
        }
        return super.read(); // reached only once a real camera is fitted
    }

    private void connect() throws IOException {
        throw new IOException("no camera stream at " + STREAM_URL);
    }

    @Override
    protected StatusLabel classify(double value) {
        return StatusLabel.INACTIVE;
    }

    @Override
    protected String formatRaw(double value) {
        return "no stream";
    }
}
