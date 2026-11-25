package farmbot.hardware;

import farmbot.model.SensorReading;
import farmbot.model.SensorType;

public class ObstacleSensor extends Sensor {
    private double distanceCm;
    private final double thresholdCm;

    public ObstacleSensor(String id, double thresholdCm) {
        super(id, "Obstacle Sensor", SensorType.OBSTACLE);
        this.thresholdCm = thresholdCm;
        this.distanceCm = thresholdCm + 1;
    }

    public void setDistanceCm(double distanceCm) {
        this.distanceCm = distanceCm;
    }

    @Override
    public SensorReading read() {
        SensorReading reading = new SensorReading(id, type, distanceCm, distanceCm + " cm");
        reading.setFlagObstacle(distanceCm < thresholdCm);
        logReading(reading);
        return reading;
    }

    @Override
    public String getDescription() {
        return "Detects obstacles within " + thresholdCm + "cm";
    }
}
