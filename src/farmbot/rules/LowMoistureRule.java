package farmbot.rules;

import farmbot.model.SensorType;
import farmbot.model.Thresholds;

import java.util.Optional;

/** Irrigation advisory when soil moisture drops below 30% (critical below 15%). */
public class LowMoistureRule extends Rule {

    public LowMoistureRule() {
        super("LOW_MOISTURE", "Low soil moisture");
    }

    @Override
    public Optional<Recommendation> evaluate(RuleContext context) {
        return context.latest(SensorType.SOIL_MOISTURE)
                .filter(reading -> reading.getValue() < Thresholds.MOISTURE_LOW)
                .map(reading -> {
                    double value = reading.getValue();
                    boolean critical = value < Thresholds.MOISTURE_CRITICAL;
                    return recommend(
                            critical ? Severity.CRITICAL : Severity.WARNING,
                            "Irrigation advisory",
                            String.format("Soil moisture at %s is %.1f%%, below the %.0f%% threshold",
                                    reading.getSensorId(), value, Thresholds.MOISTURE_LOW),
                            critical ? "Irrigate this sector immediately"
                                     : "Sprayer engaged; schedule irrigation for this sector");
                });
    }
}
