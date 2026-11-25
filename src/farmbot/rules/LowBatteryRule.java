package farmbot.rules;

import farmbot.model.SensorType;
import farmbot.model.Thresholds;

import java.util.Optional;

/** Advises returning to the dock below 20% battery; critical below 10%. */
public class LowBatteryRule extends Rule {

    public LowBatteryRule() {
        super("LOW_BATTERY", "Low battery");
    }

    @Override
    public Optional<Recommendation> evaluate(RuleContext context) {
        return context.latest(SensorType.BATTERY)
                .filter(reading -> reading.getValue() < Thresholds.BATTERY_LOW)
                .map(reading -> {
                    boolean critical = reading.getValue() < Thresholds.BATTERY_CRITICAL;
                    return recommend(
                            critical ? Severity.CRITICAL : Severity.WARNING,
                            "Battery low",
                            String.format("Battery at %.1f%%", reading.getValue()),
                            critical ? "Stop and recharge now" : "Return to the charging dock after this row");
                });
    }
}
