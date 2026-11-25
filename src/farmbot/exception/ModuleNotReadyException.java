package farmbot.exception;

/** A mission module was asked for data or work while it is not ready (e.g. the 3D scanner stub). */
public class ModuleNotReadyException extends FarmBotException {

    private static final long serialVersionUID = 1L;

    private final String moduleId;

    public ModuleNotReadyException(String moduleId, String message) {
        super(message);
        this.moduleId = moduleId;
    }

    public String getModuleId() {
        return moduleId;
    }
}
