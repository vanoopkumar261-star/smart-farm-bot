package farmbot.user;

import farmbot.exception.AuthenticationException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registers and authenticates accounts.
 *
 * Accounts live in a thread-safe Map (the web thread may register while another
 * logs in). Passwords are never stored in the clear: {@link #hash} runs SHA-256 and
 * only the hex digest is kept, matching the spec's "the database never stores plain
 * text". Authenticating hashes the presented password and compares digests.
 *
 * Two exception paths, both exercised by the demo:
 *   - registering a name that already exists throws IllegalArgumentException;
 *   - an unknown user or a wrong password throws AuthenticationException, with a
 *     deliberately vague message so it never reveals which of the two was wrong.
 */
public final class AuthService {

    private final ConcurrentHashMap<String, User> usersByName = new ConcurrentHashMap<>();

    /** Creates the service with the two demo accounts from the spec already registered. */
    public static AuthService withDemoUsers() {
        AuthService service = new AuthService();
        service.register("anoop", "tech123", Role.TECHNICIAN);
        service.register("farmer", "farmer123", Role.FARMER);
        return service;
    }

    /**
     * Adds a new account.
     *
     * @throws IllegalArgumentException if the username is already taken
     */
    public User register(String username, String password, Role role) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (password == null || password.length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters");
        }
        String key = username.trim();
        User user = create(key, hash(password), role);
        // putIfAbsent is atomic: two simultaneous registrations cannot both win.
        if (usersByName.putIfAbsent(key, user) != null) {
            throw new IllegalArgumentException("Username '" + key + "' is already taken");
        }
        return user;
    }

    /**
     * Checks a login.
     *
     * @return the authenticated user
     * @throws AuthenticationException if the user is unknown or the password is wrong
     */
    public User authenticate(String username, String password) throws AuthenticationException {
        User user = username == null ? null : usersByName.get(username.trim());
        // Hash and compare even when the user is unknown would be ideal to avoid timing
        // leaks; here clarity wins, but the error message stays vague either way.
        if (user == null || !user.getPasswordHash().equals(hash(password == null ? "" : password))) {
            throw new AuthenticationException("Invalid username or password");
        }
        return user;
    }

    public boolean exists(String username) {
        return username != null && usersByName.containsKey(username.trim());
    }

    public int userCount() {
        return usersByName.size();
    }

    /** Picks the concrete subclass for the role - the only place that maps role to type. */
    private static User create(String username, String passwordHash, Role role) {
        return switch (role) {
            case TECHNICIAN -> new Technician(username, passwordHash);
            case FARMER -> new Farmer(username, passwordHash);
        };
    }

    /** SHA-256, returned as a lowercase hex string. */
    static String hash(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed present on every JVM; this cannot happen.
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
