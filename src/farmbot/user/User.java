package farmbot.user;

/**
 * An account that can log in. Abstract because a plain "user" is never created:
 * every account is a Farmer or a Technician.
 *
 * The password is stored only as a SHA-256 hash (set by AuthService). The class
 * never holds the plain-text password, and getPasswordHash() is package-private
 * so only AuthService in this package can read it to check a login.
 *
 * canAccessHardware() is the one piece of behaviour that differs by role, so it is
 * abstract here and answered by each subclass - polymorphism instead of an
 * {@code if (role == TECHNICIAN)} check scattered around the code.
 */
public abstract class User {

    private final String username;
    private final String passwordHash;

    protected User(String username, String passwordHash) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash is required");
        }
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public String getUsername() {
        return username;
    }

    public abstract Role getRole();

    /** Only a Technician may use the hardware / module-bay controls. */
    public abstract boolean canAccessHardware();

    /** Package-private: only AuthService compares this against a presented password. */
    String getPasswordHash() {
        return passwordHash;
    }
}
