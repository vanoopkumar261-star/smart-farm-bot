package farmbot.web;

import farmbot.users.User;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory session store - a course demo, not a hardened auth system. */
public class SessionManager {
    private final Map<String, User> tokenToUser = new ConcurrentHashMap<>();

    public String createSession(User user) {
        String token = UUID.randomUUID().toString();
        tokenToUser.put(token, user);
        return token;
    }

    public User getUser(String token) {
        if (token == null) {
            return null;
        }
        return tokenToUser.get(token);
    }

    public void invalidate(String token) {
        if (token != null) {
            tokenToUser.remove(token);
        }
    }
}
