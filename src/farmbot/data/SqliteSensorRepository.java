package farmbot.data;

import farmbot.exception.DataAccessException;
import farmbot.model.SensorType;
import farmbot.model.StatusLabel;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JDBC implementation of {@link SensorRepository}, backed by a single SQLite file.
 *
 * Design decisions worth explaining in the viva:
 *  - One Connection is opened in the constructor and reused. SQLite is a single-file
 *    database, so a connection is cheap to keep but not safe to use from two threads
 *    at once; every public method is therefore {@code synchronized}. In this project
 *    the DbWriter thread writes and the web thread reads, and synchronising here means
 *    they can never touch the connection at the same moment.
 *  - PreparedStatement is used for every query, so values are bound as parameters and
 *    SQL injection is impossible.
 *  - Statements and result sets are opened in try-with-resources, so they close even
 *    when an exception is thrown.
 *  - Batch inserts run inside one transaction (autoCommit off, then commit), which is
 *    far faster than committing each row and is all-or-nothing.
 *  - Every SQLException is caught and rethrown as an unchecked DataAccessException, so
 *    JDBC types never leak past this class.
 */
public final class SqliteSensorRepository implements SensorRepository {

    private static final String INSERT_SQL =
            "INSERT INTO sensor_readings"
          + " (sensor_id, sensor_type, value, unit, raw, status, recorded_at)"
          + " VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_COLS =
            "sensor_id, sensor_type, value, unit, raw, status, recorded_at";

    private final Connection connection;

    /**
     * Opens (creating if necessary) the SQLite database at the given path.
     *
     * @param dbPath e.g. {@code data/farmbot.db}
     */
    public SqliteSensorRepository(String dbPath) {
        try {
            // The sqlite-jdbc driver registers itself via the ServiceLoader, so no
            // Class.forName is needed. The file is created if it does not exist.
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        } catch (SQLException e) {
            throw new DataAccessException("Could not open database at " + dbPath, e);
        }
    }

    @Override
    public synchronized void initSchema() {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS sensor_readings ("
              + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
              + " sensor_id TEXT NOT NULL,"
              + " sensor_type TEXT NOT NULL,"
              + " value REAL NOT NULL,"
              + " unit TEXT,"
              + " raw TEXT,"
              + " status TEXT NOT NULL,"
              + " recorded_at TEXT NOT NULL)");
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS obstacle_events ("
              + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
              + " event_time TEXT NOT NULL,"
              + " distance_cm REAL,"
              + " outcome TEXT NOT NULL)");
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS fault_log ("
              + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
              + " fault_time TEXT NOT NULL,"
              + " source TEXT NOT NULL,"
              + " exception TEXT NOT NULL,"
              + " message TEXT)");
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS modules ("
              + " module_id TEXT PRIMARY KEY,"
              + " name TEXT NOT NULL,"
              + " state TEXT NOT NULL,"
              + " mounted_at TEXT NOT NULL)");
            // No users table: authentication is in-memory by design (AuthService), while
            // sensor readings, faults and obstacle events persist. We don't create a table
            // we never write to.
            // One index makes the per-sensor history query fast.
            st.executeUpdate(
                "CREATE INDEX IF NOT EXISTS idx_readings_sensor"
              + " ON sensor_readings (sensor_id, id)");
        } catch (SQLException e) {
            throw new DataAccessException("Could not create the database schema", e);
        }
    }

    @Override
    public synchronized void insert(Reading reading) {
        try (PreparedStatement ps = connection.prepareStatement(INSERT_SQL)) {
            bindReading(ps, reading);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not insert reading " + reading, e);
        }
    }

    @Override
    public synchronized void insertBatch(List<Reading> readings) {
        if (readings == null || readings.isEmpty()) {
            return;
        }
        try {
            connection.setAutoCommit(false);
            try (PreparedStatement ps = connection.prepareStatement(INSERT_SQL)) {
                for (Reading reading : readings) {
                    bindReading(ps, reading);
                    ps.addBatch();
                }
                ps.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not batch-insert " + readings.size() + " readings", e);
        }
    }

    @Override
    public synchronized List<Reading> findBySensor(String sensorId, int limit) {
        // Take the latest `limit` rows, then flip to oldest-first for charting.
        String sql =
                "SELECT " + SELECT_COLS + " FROM ("
              + "  SELECT " + SELECT_COLS + ", id FROM sensor_readings"
              + "  WHERE sensor_id = ? ORDER BY id DESC LIMIT ?"
              + " ) ORDER BY id ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, sensorId);
            ps.setInt(2, Math.max(0, limit));
            try (ResultSet rs = ps.executeQuery()) {
                List<Reading> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapRow(rs));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read history for sensor " + sensorId, e);
        }
    }

    @Override
    public synchronized Map<String, Reading> latestPerSensor() {
        // For each sensor, the row with the greatest id is the most recent reading.
        String sql =
                "SELECT " + SELECT_COLS + " FROM sensor_readings r"
              + " WHERE id = (SELECT MAX(id) FROM sensor_readings WHERE sensor_id = r.sensor_id)"
              + " ORDER BY sensor_id";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            Map<String, Reading> latest = new LinkedHashMap<>();
            while (rs.next()) {
                Reading reading = mapRow(rs);
                latest.put(reading.getSensorId(), reading);
            }
            return latest;
        } catch (SQLException e) {
            throw new DataAccessException("Could not read latest readings", e);
        }
    }

    @Override
    public synchronized int count() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM sensor_readings");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DataAccessException("Could not count readings", e);
        }
    }

    @Override
    public synchronized void recordFault(String source, String exception, String message) {
        String sql = "INSERT INTO fault_log (fault_time, source, exception, message) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, LocalDateTime.now().format(Reading.TIME_FORMAT));
            ps.setString(2, source);
            ps.setString(3, exception);
            ps.setString(4, message);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not record fault from " + source, e);
        }
    }

    @Override
    public synchronized List<FaultRecord> recentFaults(int limit) {
        String sql = "SELECT fault_time, source, exception, message FROM fault_log"
                   + " ORDER BY id DESC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, Math.max(0, limit));
            try (ResultSet rs = ps.executeQuery()) {
                List<FaultRecord> faults = new ArrayList<>();
                while (rs.next()) {
                    faults.add(new FaultRecord(
                            LocalDateTime.parse(rs.getString("fault_time"), Reading.TIME_FORMAT),
                            rs.getString("source"),
                            rs.getString("exception"),
                            rs.getString("message")));
                }
                return faults;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read fault log", e);
        }
    }

    @Override
    public synchronized void recordObstacleEvent(Double distanceCm, String outcome) {
        String sql = "INSERT INTO obstacle_events (event_time, distance_cm, outcome) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, LocalDateTime.now().format(Reading.TIME_FORMAT));
            if (distanceCm == null) {
                ps.setNull(2, java.sql.Types.REAL);
            } else {
                ps.setDouble(2, distanceCm);
            }
            ps.setString(3, outcome);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not record obstacle event", e);
        }
    }

    @Override
    public synchronized List<ObstacleEvent> obstacleEvents() {
        String sql = "SELECT event_time, distance_cm, outcome FROM obstacle_events ORDER BY id DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<ObstacleEvent> events = new ArrayList<>();
            while (rs.next()) {
                double distance = rs.getDouble("distance_cm");
                Double distanceCm = rs.wasNull() ? null : distance;
                events.add(new ObstacleEvent(
                        LocalDateTime.parse(rs.getString("event_time"), Reading.TIME_FORMAT),
                        distanceCm,
                        rs.getString("outcome")));
            }
            return events;
        } catch (SQLException e) {
            throw new DataAccessException("Could not read obstacle events", e);
        }
    }

    @Override
    public synchronized void close() {
        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not close the database connection", e);
        }
    }

    /** Binds a Reading's seven fields onto the INSERT statement, in column order. */
    private static void bindReading(PreparedStatement ps, Reading reading) throws SQLException {
        ps.setString(1, reading.getSensorId());
        ps.setString(2, reading.getSensorType().name());
        ps.setDouble(3, reading.getValue());
        ps.setString(4, reading.getUnit());
        ps.setString(5, reading.getRaw());
        ps.setString(6, reading.getLabel().name());
        ps.setString(7, reading.getRecordedAtText());
    }

    /** Rebuilds a Reading from one result-set row. */
    private static Reading mapRow(ResultSet rs) throws SQLException {
        SensorType type = SensorType.valueOf(rs.getString("sensor_type"));
        StatusLabel label = StatusLabel.valueOf(rs.getString("status"));
        LocalDateTime recordedAt = LocalDateTime.parse(rs.getString("recorded_at"), Reading.TIME_FORMAT);
        return new Reading(
                rs.getString("sensor_id"),
                type,
                rs.getDouble("value"),
                rs.getString("unit"),
                rs.getString("raw"),
                label,
                recordedAt);
    }
}
