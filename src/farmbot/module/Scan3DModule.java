package farmbot.module;

import farmbot.data.Reading;
import farmbot.exception.ModuleNotReadyException;
import farmbot.model.ModuleState;

/**
 * STUB for a future 3D crop scanner. It mounts like any module, but reports
 * INACTIVE and refuses every read with ModuleNotReadyException. The bot catches
 * that and shows the card as INACTIVE; nothing crashes.
 *
 * It shows that a planned feature can be wired in before its hardware exists.
 */
public class Scan3DModule extends BaseModule {

    public Scan3DModule(String id) {
        super(id, "3D Crop Scanner");
    }

    @Override
    public String getDescription() {
        return "Planned: 3D point-cloud scans of crop rows (not fitted yet)";
    }

    @Override
    public void mount() {
        super.mount();                  // records mountedAt like any other module
        setState(ModuleState.INACTIVE); // ...but stays inactive
    }

    @Override
    public Reading read() throws ModuleNotReadyException {
        throw new ModuleNotReadyException(getId(), "3D scanner hardware is not fitted (stub)");
    }

    @Override
    protected Reading createReading() throws ModuleNotReadyException {
        throw new ModuleNotReadyException(getId(), "3D scanner hardware is not fitted (stub)");
    }
}
