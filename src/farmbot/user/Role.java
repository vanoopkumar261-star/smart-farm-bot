package farmbot.user;

/**
 * What a logged-in account is allowed to see. The dashboard shows the Module Bay
 * (hardware) controls only to a TECHNICIAN; a FARMER sees the read-only view.
 */
public enum Role {
    FARMER("Farmer"),
    TECHNICIAN("Technician");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Parses the role sent by the register form; defaults to FARMER on anything unknown. */
    public static Role fromText(String text) {
        if (text != null) {
            for (Role role : values()) {
                if (role.name().equalsIgnoreCase(text.trim())) {
                    return role;
                }
            }
        }
        throw new IllegalArgumentException("Unknown role '" + text + "'");
    }
}
