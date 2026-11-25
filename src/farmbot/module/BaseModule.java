package farmbot.module;

import farmbot.data.Reading;
import farmbot.exception.ModuleNotReadyException;
import farmbot.model.ModuleState;
import farmbot.model.SensorType;
import farmbot.model.StatusLabel;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * Shared code for every mission module: identity, mount/unmount life cycle,
 * and the "refuse to read when not ready" check.
 *
 * Subclasses implement createReading() and getDescription(), and may override
 * the update() hook. The interface says WHAT a module can do; this abstract
 * class gives the common HOW, so the modules don't repeat it.
 */
public abstract class BaseModule implements MissionModule {

    private final String id;
    private final String name;

    // volatile: written by the control thread, read by the poller and web threads.
    private volatile ModuleState state = ModuleState.INACTIVE;
    private volatile LocalDateTime mountedAt;

    protected BaseModule(String id, String name) {
        if (id == null || id.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Module id and name are required");
        }
        this.id = id;
        this.name = name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ModuleState getState() {
        return state;
    }

    @Override
    public LocalDateTime getMountedAt() {
        return mountedAt;
    }

    @Override
    public void mount() {
        mountedAt = LocalDateTime.now();
        setState(ModuleState.MOUNTED);
    }

    @Override
    public void unmount() {
        mountedAt = null;
        setState(ModuleState.INACTIVE);
    }

    @Override
    public Reading read() throws ModuleNotReadyException {
        if (!state.isReady()) {
            throw new ModuleNotReadyException(id, name + " is " + state);
        }
        return createReading();
    }

    /** Hook: does nothing by default. Modules that react to the soil override it. */
    @Override
    public void update(Map<SensorType, Reading> latest) {
        // no-op by default
    }

    /** Build this module's Reading. Only called when the module is ready. */
    protected abstract Reading createReading() throws ModuleNotReadyException;

    protected void setState(ModuleState newState) {
        this.state = Objects.requireNonNull(newState, "state");
    }

    /** Helper so every module builds its Reading the same way. */
    protected Reading buildReading(double value, String unit, String raw, StatusLabel label) {
        return new Reading(id, SensorType.MODULE, value, unit, raw, label, LocalDateTime.now());
    }

    @Override
    public String toString() {
        return "Module " + id + " (" + name + ") " + state;
    }
}
