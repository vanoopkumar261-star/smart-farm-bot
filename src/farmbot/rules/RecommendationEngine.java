package farmbot.rules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Runs every Rule against the current context and collects the advisories,
 * most severe first.
 *
 * Each rule runs inside its own try/catch: a bug in one rule is logged and
 * skipped, and the other rules still run.
 */
public class RecommendationEngine {

    private final List<Rule> rules;

    public RecommendationEngine(List<Rule> rules) {
        this.rules = List.copyOf(rules); // unmodifiable snapshot
    }

    public static RecommendationEngine withDefaultRules() {
        return new RecommendationEngine(List.of(
                new LowMoistureRule(), new ObstacleDensityRule(), new LowBatteryRule()));
    }

    public List<Recommendation> evaluate(RuleContext context) {
        List<Recommendation> results = new ArrayList<>();
        for (Rule rule : rules) {
            try {
                rule.evaluate(context).ifPresent(results::add);
            } catch (RuntimeException e) {
                System.err.println("[rules] " + rule.getId() + " failed and was skipped: " + e);
            }
        }
        results.sort(Comparator.comparing(Recommendation::getSeverity).reversed());
        return results;
    }

    public List<Rule> getRules() {
        return rules;
    }
}
