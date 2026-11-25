package farmbot.user;

/** A technician: full access, including the hardware / module-bay controls. */
public final class Technician extends User {

    public Technician(String username, String passwordHash) {
        super(username, passwordHash);
    }

    @Override
    public Role getRole() {
        return Role.TECHNICIAN;
    }

    @Override
    public boolean canAccessHardware() {
        return true;
    }
}
