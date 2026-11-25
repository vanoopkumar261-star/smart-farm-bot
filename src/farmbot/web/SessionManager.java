package farmbot.web;

import farmbot.user.User;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Issues and checks the opaque session tokens the browser sends in the
 * {@code X-Auth-Token} header after logging in.
 *
 * A token is 32 random bytes as hex; the map from token to the logged-in User is a
 * ConcurrentHashMap because many request threads touch it at once. There is no
 * server-side expiry in this course project - logging out (or restarting the server)
 * clears the session.
 */
public final class SessionManager {

    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<String, User> sessions = new ConcurrentHashMap<>();

    /** Starts a session for a user and returns its token. */
    public String open(User user) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        sessions.put(token, user);
        return token;
    }

    /** The user behind a token, if the token is valid. */
    public Optional<User> lookup(String token) {
        return Optional.ofNullable(token == null ? null : sessions.get(token));
    }

    /** Ends a session (logout). */
    public void close(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    public int activeSessions() {
        return sessions.size();
    }
}
