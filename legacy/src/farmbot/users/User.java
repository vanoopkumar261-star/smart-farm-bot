package farmbot.users;

import farmbot.exceptions.AuthenticationException;
import farmbot.model.Permission;

import java.util.Set;

public abstract class User {
    protected String username;
    protected String passwordHash;

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public boolean login(String inputPassword) throws AuthenticationException {
        if (!hash(inputPassword).equals(passwordHash)) {
            throw new AuthenticationException("invalid credentials");
        }
        return true;
    }

    public static String hash(String raw) {
        return Integer.toHexString(raw.hashCode());
    }

    public String getUsername() {
        return username;
    }

    public abstract Set<Permission> getPermissions();

    public abstract String getDashboardView();
}
