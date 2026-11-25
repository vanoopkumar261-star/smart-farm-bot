package farmbot.hardware;

import farmbot.model.SensorReading;
import farmbot.model.SensorType;
import farmbot.sim.SensorDataSource;

public class LineSensor extends Sensor {
    private final SensorDataSource source;
    private int leftSensor;
    private int centerSensor;
    private int rightSensor;

    public LineSensor(String id, SensorDataSource source) {
        super(id, "Line Sensor", SensorType.LINE);
        this.source = source;
    }

    @Override
    public SensorReading read() {
        int encoded = (int) source.poll(id);
        leftSensor = (encoded / 100) % 10;
        centerSensor = (encoded / 10) % 10;
        rightSensor = encoded % 10;
        String display = "L=" + leftSensor + " C=" + centerSensor + " R=" + rightSensor;
        SensorReading reading = new SensorReading(id, type, encoded, display);
        logReading(reading);
        return reading;
    }

    public int getLeftSensor() {
        return leftSensor;
    }

    public int getCenterSensor() {
        return centerSensor;
    }

    public int getRightSensor() {
        return rightSensor;
    }

    public boolean isLineLost() {
        return leftSensor == 0 &&
               centerSensor == 0 &&
               rightSensor == 0;
    }

    @Override
    public String getDescription() {
        return "Detects the guide line under the chassis (left/center/right)";
    }
}
