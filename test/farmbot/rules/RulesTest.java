package farmbot.rules;

import farmbot.data.Reading;
import farmbot.model.SensorType;
import farmbot.model.StatusLabel;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** One test per advisory Rule, as the spec requires. */
class RulesTest {

    private static RuleContext context(SensorType type, double value, int obstacleEvents) {
        Map<SensorType, Reading> latest = new EnumMap<>(SensorType.class);
        latest.put(type, new Reading("X-01", type, value, "", "", StatusLabel.NORMAL, LocalDateTime.now()));
        return new RuleContext(latest, obstacleEvents);
    }

    // ---- LowMoistureRule ----

    @Test
    void lowMoistureWarnsBelowThreshold() {
        Optional<Recommendation> rec = new LowMoistureRule().evaluate(context(SensorType.SOIL_MOISTURE, 25.0, 0));
        assertTrue(rec.isPresent());
        assertEquals(Severity.WARNING, rec.get().getSeverity());
    }

    @Test
    void lowMoistureIsCriticalWhenVeryDry() {
        Optional<Recommendation> rec = new LowMoistureRule().evaluate(context(SensorType.SOIL_MOISTURE, 10.0, 0));
        assertTrue(rec.isPresent());
        assertEquals(Severity.CRITICAL, rec.get().getSeverity());
    }

    @Test
    void lowMoistureSilentWhenWet() {
        assertTrue(new LowMoistureRule().evaluate(context(SensorType.SOIL_MOISTURE, 42.0, 0)).isEmpty());
    }

    // ---- LowBatteryRule ----

    @Test
    void lowBatteryWarnsBelowTwentyPercent() {
        Optional<Recommendation> rec = new LowBatteryRule().evaluate(context(SensorType.BATTERY, 15.0, 0));
        assertTrue(rec.isPresent());
        assertEquals(Severity.WARNING, rec.get().getSeverity());
    }

    @Test
    void lowBatterySilentWhenCharged() {
        assertTrue(new LowBatteryRule().evaluate(context(SensorType.BATTERY, 80.0, 0)).isEmpty());
    }

    // ---- ObstacleDensityRule ----

    @Test
    void obstacleDensityWarnsAtThreshold() {
        Optional<Recommendation> rec = new ObstacleDensityRule().evaluate(new RuleContext(Map.of(), 3));
        assertTrue(rec.isPresent());
        assertEquals(Severity.WARNING, rec.get().getSeverity());
    }

    @Test
    void obstacleDensitySilentBelowThreshold() {
        assertTrue(new ObstacleDensityRule().evaluate(new RuleContext(Map.of(), 2)).isEmpty());
    }
}
