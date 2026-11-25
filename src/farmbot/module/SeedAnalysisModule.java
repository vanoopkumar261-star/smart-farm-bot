package farmbot.module;

import farmbot.data.Reading;
import farmbot.exception.ModuleNotReadyException;
import farmbot.model.SensorType;
import farmbot.model.StatusLabel;

import java.util.Map;

/**
 * Seed analysis module. On each tick it scores how suitable the soil currently is
 * for germination (0-100%), from the base soil temperature and moisture sensors.
 *   - ideal soil temperature: 25 C (lose 4 points per degree away from it)
 *   - ideal moisture: 40% or more (lose 1.5 points per % below it)
 *
 * Reading value = germination suitability %. Raw = number of samples analysed.
 */
public class SeedAnalysisModule extends BaseModule {

    private static final double IDEAL_TEMP = 25.0;
    private static final double IDEAL_MOISTURE = 40.0;

    private double suitability;
    private int samplesAnalysed;

    public SeedAnalysisModule(String id) {
        super(id, "Seed Analysis Unit");
    }

    @Override
    public String getDescription() {
        return "Scores germination suitability from soil temperature and moisture";
    }

    @Override
    public synchronized void update(Map<SensorType, Reading> latest) {
        Reading temperature = latest.get(SensorType.SOIL_TEMPERATURE);
        Reading moisture = latest.get(SensorType.SOIL_MOISTURE);
        if (!getState().isReady() || temperature == null || moisture == null) {
            return;
        }
        double score = 100.0
                - Math.abs(temperature.getValue() - IDEAL_TEMP) * 4.0
                - Math.max(0.0, IDEAL_MOISTURE - moisture.getValue()) * 1.5;
        suitability = Math.max(0.0, Math.min(100.0, score));
        samplesAnalysed++;
    }

    @Override
    protected synchronized Reading createReading() throws ModuleNotReadyException {
        if (samplesAnalysed == 0) {
            throw new ModuleNotReadyException(getId(), "No soil sample analysed yet");
        }
        StatusLabel label = suitability < 50.0 ? StatusLabel.POOR : StatusLabel.SUITABLE;
        double rounded = Math.round(suitability * 10.0) / 10.0;
        return buildReading(rounded, "%", "samples=" + samplesAnalysed, label);
    }
}
