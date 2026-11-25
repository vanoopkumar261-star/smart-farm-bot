package farmbot.rules;

import farmbot.model.BotState;
import farmbot.model.Recommendation;

import java.util.ArrayList;
import java.util.List;

public class RecommendationEngine {
    private final List<Rule> rules = new ArrayList<>();

    public RecommendationEngine() {
        rules.add(new LowMoistureRule(30.0));
        rules.add(new ObstacleDensityRule(5));
        rules.add(new PathLossRule());
    }

    public List<Recommendation> runAll(BotState state) {
        List<Recommendation> results = new ArrayList<>();
        for (Rule rule : rules) {
            Recommendation rec = rule.evaluate(state);
            if (rec != null) {
                results.add(rec);
            }
        }
        return results;
    }
}
