package farmbot.module;

import farmbot.data.Reading;
import farmbot.model.ModuleState;
import farmbot.model.SensorType;
import farmbot.model.StatusLabel;
import farmbot.model.Thresholds;

import java.util.Map;

/**
 * Irrigation sprayer. Switches itself ON when soil moisture drops below 30% and
 * OFF again above 35%. The 5-point gap (hysteresis) stops it flickering on and
 * off when the moisture hovers around 30%.
 *
 * Reading value = flow rate in L/min (0 when idle). Raw = tank level.
 */
public class SprayerModule extends BaseModule {

    private static final double FLOW_RATE = 1.2;           // L/min while spraying
    private static final double TANK_DRAIN_PER_TICK = 0.02; // % of tank per poll (~83 min to empty;
                                                            // keeps the sprayer ACTIVE through a demo)

    // Guarded by "this": the control thread drains it while a poller thread reads it.
    private double tankLevel = 100.0;

    public SprayerModule(String id) {
        super(id, "Irrigation Sprayer");
    }

    @Override
    public String getDescription() {
        return "Sprays water when soil moisture falls below " + (int) Thresholds.MOISTURE_LOW + "%";
    }

    @Override
    public synchronized void update(Map<SensorType, Reading> latest) {
        if (!getState().isReady()) {
            return;
        }
        Reading moisture = latest.get(SensorType.SOIL_MOISTURE);
        if (moisture != null) {
            if (moisture.getValue() < Thresholds.MOISTURE_LOW) {
                setState(ModuleState.ACTIVE);
            } else if (moisture.getValue() >= Thresholds.MOISTURE_SPRAY_STOP) {
                setState(ModuleState.MOUNTED);
            }
        }
        if (getState() == ModuleState.ACTIVE) {
            tankLevel = Math.max(0.0, tankLevel - TANK_DRAIN_PER_TICK);
            if (tankLevel == 0.0) {
                setState(ModuleState.FAULT); // empty tank: needs a technician
            }
        }
    }

    @Override
    protected synchronized Reading createReading() {
        boolean spraying = getState() == ModuleState.ACTIVE;
        StatusLabel label;
        if (tankLevel < 20.0) {
            label = StatusLabel.TANK_LOW;
        } else {
            label = spraying ? StatusLabel.ACTIVE : StatusLabel.IDLE;
        }
        return buildReading(spraying ? FLOW_RATE : 0.0, "L/min",
                String.format("tank=%.1f%%", tankLevel), label);
    }

    public synchronized double getTankLevel() {
        return tankLevel;
    }

    /** Technician action: refill the tank and clear an empty-tank fault. */
    public synchronized void refill() {
        tankLevel = 100.0;
        if (getState() == ModuleState.FAULT) {
            setState(ModuleState.MOUNTED);
        }
    }
}
