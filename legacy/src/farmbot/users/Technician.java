package farmbot.users;

import farmbot.model.Permission;

import java.util.EnumSet;
import java.util.Set;

public class Technician extends User {
    public Technician(String username, String passwordHash) {
        super(username, passwordHash);
    }

    @Override
    public Set<Permission> getPermissions() {
        return EnumSet.of(
            Permission.START_STOP,
            Permission.VIEW_FIELD_STATUS,
            Permission.CALIBRATE_SENSORS,
            Permission.VIEW_RAW_LOGS
        );
    }

    @Override
    public String getDashboardView() {
        return "TechnicianDashboard";
    }
}
