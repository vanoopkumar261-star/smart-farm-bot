package farmbot.sim;

/**
 * Backs the interactive console mode: LEFT/CENTER/RIGHT values typed at the
 * Scanner prompt are injected here, and LineSensor.read() polls this same
 * seam it would use for an autorun - no special-casing of manual input.
 */
public class ManualSource implements SensorDataSource {
    private int left;
    private int center;
    private int right;

    public void inject(int left, int center, int right) {
        this.left = left;
        this.center = center;
        this.right = right;
    }

    @Override
    public double poll(String sensorId) {
        return (left * 100) + (center * 10) + right;
    }
}
