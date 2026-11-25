package farmbot.sim;

public class SimulatedSource implements SensorDataSource {
    private final Track track;
    private int position = 0;

    public SimulatedSource(Track track) {
        this.track = track;
    }

    public void advance() {
        position++;
    }

    public int getPosition() {
        return position;
    }

    @Override
    public double poll(String sensorId) {
        int[] row = track.getRow(position);
        return (row[0] * 100) + (row[1] * 10) + row[2];
    }
}
