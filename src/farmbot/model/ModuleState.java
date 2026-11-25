package farmbot.model;

/** Life-cycle state of a mission module mounted on the base rover. */
public enum ModuleState {
    MOUNTED,   // attached and ready, but not doing work right now
    ACTIVE,    // doing work (e.g. the sprayer is spraying)
    INACTIVE,  // attached but not usable (e.g. the 3D scanner stub)
    FAULT;     // failed and needs a technician

    /** Only ready modules are polled for readings. */
    public boolean isReady() {
        return this == MOUNTED || this == ACTIVE;
    }
}
