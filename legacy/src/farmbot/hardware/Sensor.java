package farmbot.hardware;

import farmbot.exceptions.SensorFailureException;
import farmbot.model.SensorReading;
import farmbot.model.SensorType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public abstract class Sensor extends Component {
    private static final int HISTORY_CAPACITY = 100;

    protected SensorType type;
    private final Deque<SensorReading> history = new ArrayDeque<>();

    public Sensor(String id, String name, SensorType type) {
        super(id, name);
        this.type = type;
    }

    public abstract SensorReading read() throws SensorFailureException;

    public boolean isActive() {
        return "OK".equals(status);
    }

    protected void logReading(SensorReading reading) {
        if (history.size() >= HISTORY_CAPACITY) {
            history.removeFirst();
        }
        history.addLast(reading);
    }

    public List<SensorReading> getHistory() {
        return new ArrayList<>(history);
    }

    /**
     * Concrete helper preserved from the original starter code's call
     * sites: reads the sensor and prints the outcome, catching a failed
     * read instead of letting it propagate.
     */
    public void readData() {
        try {
            SensorReading reading = read();
            System.out.println(name + ": " + reading.getDisplay());
        } catch (SensorFailureException e) {
            System.out.println(name + ": [FAULT] " + e.getMessage());
        }
    }
}
