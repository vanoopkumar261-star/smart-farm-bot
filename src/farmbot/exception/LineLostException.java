package farmbot.exception;

/** All three line-sensor eyes see no line. The bot recovers by switching to SearchStrategy. */
public class LineLostException extends FarmBotException {

    private static final long serialVersionUID = 1L;

    public LineLostException(String message) {
        super(message);
    }
}
