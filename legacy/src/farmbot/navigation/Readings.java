package farmbot.navigation;

import farmbot.model.SensorReading;
import farmbot.model.SensorType;

import java.util.List;

/** Shared lookup helper so every strategy can find a reading by sensor type. */
final class Readings {
    private Readings() {
    }

    static SensorReading find(List<SensorReading> readings, SensorType type) {
        for (SensorReading r : readings) {
            if (r.getSensorType() == type) {
                return r;
            }
        }
        return null;
    }
}
