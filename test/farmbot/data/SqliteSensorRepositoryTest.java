package farmbot.data;

import farmbot.model.SensorType;
import farmbot.model.StatusLabel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** JDBC round-trip: insert then read back, against a throwaway temporary database. */
class SqliteSensorRepositoryTest {

    private static Reading soil(double value, String time) {
        return new Reading("SOIL-01", SensorType.SOIL_MOISTURE, value, "%", "",
                StatusLabel.NORMAL, LocalDateTime.parse(time, Reading.TIME_FORMAT));
    }

    @Test
    void insertsAndReadsBackHistoryOldestFirst(@TempDir Path tmp) {
        Path db = tmp.resolve("repo.db");
        try (SensorRepository repo = new SqliteSensorRepository(db.toString())) {
            repo.initSchema();
            repo.insert(soil(40.0, "2026-11-23 09:00:00"));
            repo.insertBatch(List.of(
                    soil(39.0, "2026-11-23 09:01:00"),
                    soil(38.0, "2026-11-23 09:02:00")));

            assertEquals(3, repo.count());

            List<Reading> history = repo.findBySensor("SOIL-01", 10);
            assertEquals(3, history.size());
            assertEquals(40.0, history.get(0).getValue(), 0.001, "oldest first");
            assertEquals(38.0, history.get(2).getValue(), 0.001, "newest last");

            Map<String, Reading> latest = repo.latestPerSensor();
            assertNotNull(latest.get("SOIL-01"));
            assertEquals(38.0, latest.get("SOIL-01").getValue(), 0.001);
        }
    }

    @Test
    void recordsAndReadsBackFaults(@TempDir Path tmp) {
        Path db = tmp.resolve("faults.db");
        try (SensorRepository repo = new SqliteSensorRepository(db.toString())) {
            repo.initSchema();
            repo.recordFault("CAM-01", "ConnectException", "camera not connected");
            List<FaultRecord> faults = repo.recentFaults(10);
            assertEquals(1, faults.size());
            assertEquals("CAM-01", faults.get(0).getSource());
            assertEquals("ConnectException", faults.get(0).getException());
        }
    }

    @Test
    void recordsObstacleEventsIncludingAnUnmeasuredOne(@TempDir Path tmp) {
        Path db = tmp.resolve("obstacles.db");
        try (SensorRepository repo = new SqliteSensorRepository(db.toString())) {
            repo.initSchema();
            repo.recordObstacleEvent(15.0, "HALTED");
            repo.recordObstacleEvent(null, "UNDETECTED_COLLISION"); // no distance measured
            List<ObstacleEvent> events = repo.obstacleEvents();
            assertEquals(2, events.size());
            long undetected = events.stream().filter(e -> !e.wasDetected()).count();
            assertEquals(1, undetected);
            ObstacleEvent collision = events.stream().filter(e -> !e.wasDetected()).findFirst().orElseThrow();
            assertNull(collision.getDistanceCm());
        }
    }
}
