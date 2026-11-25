package farmbot.sim;

public interface SensorDataSource {
    double poll(String sensorId);
}
