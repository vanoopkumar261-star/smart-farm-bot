package farmbot.exception;

/** Unknown username or wrong password. The message is deliberately vague so it doesn't reveal which. */
public class AuthenticationException extends FarmBotException {

    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }
}
