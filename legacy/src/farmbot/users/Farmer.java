package farmbot.users;

import farmbot.model.Permission;

import java.util.EnumSet;
import java.util.Set;

public class Farmer extends User {
    public Farmer(String username, String passwordHash) {
        super(username, passwordHash);
    }

    @Override
    public Set<Permission> getPermissions() {
        return EnumSet.of(Permission.START_STOP, Permission.VIEW_FIELD_STATUS);
    }

    @Override
    public String getDashboardView() {
        return "FarmerDashboard";
    }
}
