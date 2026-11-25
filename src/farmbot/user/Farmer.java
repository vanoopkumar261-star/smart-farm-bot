package farmbot.user;

/** A farmer: sees the live dashboard, but not the hardware / module-bay controls. */
public final class Farmer extends User {

    public Farmer(String username, String passwordHash) {
        super(username, passwordHash);
    }

    @Override
    public Role getRole() {
        return Role.FARMER;
    }

    @Override
    public boolean canAccessHardware() {
        return false;
    }
}
