package farmbot.exception;

/**
 * Wraps every SQLException coming out of the data layer.
 * Unchecked, so the SensorRepository interface does not expose JDBC types
 * (java.sql.SQLException) to the rest of the program. The original exception
 * is kept as the cause, so no information is lost.
 */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
