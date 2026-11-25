package farmbot.data;

import java.util.List;
import java.util.Map;

/**
 * The data-access boundary for the whole program (the DAO pattern).
 *
 * Nothing above this interface knows that the store is SQLite: the methods speak
 * only in Reading objects and plain types, and every JDBC detail (Connection,
 * PreparedStatement, SQLException) is hidden inside the implementation. That means
 * the SensorPoller, DbWriter and web layer depend on this interface, not on JDBC,
 * so the store could be swapped (e.g. for an in-memory fake in a test) without
 * touching them.
 *
 * It extends AutoCloseable so a caller can open it in a try-with-resources block
 * and be sure the database connection is always closed.
 */
public interface SensorRepository extends AutoCloseable {

    /** Creates every table if it does not already exist. Safe to call repeatedly. */
    void initSchema();

    /** Inserts one reading into sensor_readings. */
    void insert(Reading reading);

    /**
     * Inserts many readings in a single batched transaction. This is what the
     * DbWriter consumer thread uses to drain its queue efficiently.
     */
    void insertBatch(List<Reading> readings);

    /**
     * The most recent {@code limit} readings for one sensor, oldest first, so the
     * dashboard can chart them left to right. Backs GET /api/history.
     */
    List<Reading> findBySensor(String sensorId, int limit);

    /** The single latest reading for every sensor, keyed by sensor id. Backs GET /api/sensors. */
    Map<String, Reading> latestPerSensor();

    /** Total number of rows in sensor_readings. Used to decide whether seeding is needed. */
    int count();

    /** Appends one row to fault_log (timestamped now). Backs the Faults panel. */
    void recordFault(String source, String exception, String message);

    /** The most recent {@code limit} faults, newest first. Backs GET /api/faults. */
    List<FaultRecord> recentFaults(int limit);

    /** Appends one row to obstacle_events (timestamped now). {@code distanceCm} may be null. */
    void recordObstacleEvent(Double distanceCm, String outcome);

    /** Every obstacle event, newest first. Backs GET /api/obstacles. */
    List<ObstacleEvent> obstacleEvents();

    /** Closes the underlying database connection. Does not throw a checked exception. */
    @Override
    void close();
}
