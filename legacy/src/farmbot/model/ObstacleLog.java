package farmbot.model;

import java.util.ArrayList;
import java.util.List;

public class ObstacleLog {
    private final List<ObstacleEvent> events = new ArrayList<>();

    public void record(boolean detected) {
        events.add(new ObstacleEvent(detected));
    }

    public int countDetected() {
        int count = 0;
        for (ObstacleEvent e : events) {
            if (e.isDetected()) {
                count++;
            }
        }
        return count;
    }

    public int countCollisions() {
        int count = 0;
        for (ObstacleEvent e : events) {
            if (!e.isDetected()) {
                count++;
            }
        }
        return count;
    }

    public int countInLastNMoves(int n) {
        int from = Math.max(0, events.size() - n);
        return events.subList(from, events.size()).size();
    }

    public int size() {
        return events.size();
    }
}
