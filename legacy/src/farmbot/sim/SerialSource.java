package farmbot.sim;

/**
 * Stub for a real hardware link (semester-4 work). No physical bot exists
 * yet, so this deliberately isn't implemented - swapping this in for
 * SimulatedSource is the intended next step once hardware is available.
 */
public class SerialSource implements SensorDataSource {
    @Override
    public double poll(String sensorId) {
        throw new UnsupportedOperationException("hardware link not yet implemented");
    }
}
