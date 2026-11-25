package farmbot.rules;

import java.util.Optional;

/** Too many obstacle halts in one run suggests the row is blocked or overgrown. */
public class ObstacleDensityRule extends Rule {

    private final int maxEvents;

    public ObstacleDensityRule() {
        this(3);
    }

    public ObstacleDensityRule(int maxEvents) {
        super("OBSTACLE_DENSITY", "Obstacle density");
        if (maxEvents < 1) {
            throw new IllegalArgumentException("maxEvents must be at least 1");
        }
        this.maxEvents = maxEvents;
    }

    @Override
    public Optional<Recommendation> evaluate(RuleContext context) {
        int count = context.getObstacleEventCount();
        if (count < maxEvents) {
            return Optional.empty();
        }
        return Optional.of(recommend(Severity.WARNING, "Path congested",
                count + " obstacle halts in this run (limit " + maxEvents + ")",
                "Clear debris from the row or re-route the mission"));
    }
}
