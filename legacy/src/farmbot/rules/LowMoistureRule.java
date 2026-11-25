package farmbot.rules;

import farmbot.model.BotState;
import farmbot.model.Recommendation;

public class LowMoistureRule extends Rule {
    private final double thresholdPct;

    public LowMoistureRule(double thresholdPct) {
        this.thresholdPct = thresholdPct;
    }

    @Override
    public Recommendation evaluate(BotState state) {
        if (state.getSoilMoisturePct() < thresholdPct) {
            return new Recommendation("Soil moisture low - consider irrigation");
        }
        return null;
    }
}
