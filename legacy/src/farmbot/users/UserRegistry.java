package farmbot.users;

import farmbot.exceptions.AuthenticationException;

import java.util.HashMap;
import java.util.Map;

public class UserRegistry {
    private final Map<String, User> users = new HashMap<>();

    public User register(String username, String password, String role) {
        if (users.containsKey(username)) {
            throw new IllegalArgumentException("username taken");
        }
        String hashed = User.hash(password);
        User user = "TECHNICIAN".equalsIgnoreCase(role)
                ? new Technician(username, hashed)
                : new Farmer(username, hashed);
        users.put(username, user);
        return user;
    }

    public User authenticate(String username, String password) throws AuthenticationException {
        User user = users.get(username);
        if (user == null) {
            throw new AuthenticationException("no such user");
        }
        user.login(password);
        return user;
    }
}
