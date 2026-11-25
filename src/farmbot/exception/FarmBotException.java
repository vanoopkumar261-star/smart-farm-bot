package farmbot.exception;

/**
 * Base class for every CHECKED exception in the project.
 *
 * Design rule used throughout the code:
 *  - Checked (extends FarmBotException): a condition the bot is expected to hit
 *    and must recover from, such as a lost line, a failed sensor, a module that
 *    is not ready, or a wrong password. The compiler forces callers to handle it.
 *  - Unchecked (extends RuntimeException): a programming or infrastructure
 *    error, such as an invalid motor speed or a broken database.
 */
public class FarmBotException extends Exception {

    private static final long serialVersionUID = 1L;

    public FarmBotException(String message) {
        super(message);
    }

    public FarmBotException(String message, Throwable cause) {
        super(message, cause);
    }
}
