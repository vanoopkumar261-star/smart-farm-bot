package farmbot.module;

import farmbot.data.Reading;
import farmbot.exception.ModuleNotReadyException;
import farmbot.model.ModuleState;
import farmbot.model.SensorType;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Contract for anything that mounts on top of the base rover.
 *
 * SmartFarmBot only ever talks to this interface. To add a new module (e.g. a
 * weeder), write ONE new class that implements it (normally by extending
 * BaseModule) and mount it; the bot itself does not change. That is the
 * Open/Closed Principle: open for extension, closed for modification.
 */
public interface MissionModule {

    String getId();

    String getName();

    /** One line for the Module Bay card in the UI. */
    String getDescription();

    ModuleState getState();

    /** When the module was mounted, or null if it is not mounted. */
    LocalDateTime getMountedAt();

    void mount();

    void unmount();

    /** Latest measurement from the module. Throws if the module is not ready. */
    Reading read() throws ModuleNotReadyException;

    /**
     * Called on every control tick with the latest base-sensor readings, so the
     * module can react (e.g. the sprayer switches on when the soil is dry).
     */
    void update(Map<SensorType, Reading> latest);
}
