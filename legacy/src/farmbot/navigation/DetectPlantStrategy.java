package farmbot.navigation;

import farmbot.model.Command;
import farmbot.model.Direction;
import farmbot.model.SensorReading;
import farmbot.model.SensorType;

import java.util.List;

public class DetectPlantStrategy implements NavigationStrategy {
    @Override
    public Command decide(List<SensorReading> readings) {
        SensorReading cam = Readings.find(readings, SensorType.CAMERA);
        if (cam == null) {
            // ESP32-CAM is simulated/inactive - no vision feed to detect a
            // plant with, so proceed cautiously at reduced speed instead.
            return new Command(Direction.FORWARD, 40);
        }
        return new Command(Direction.STOP, 0);
    }

    @Override
    public String getName() {
        return "DETECT_PLANT";
    }
}
