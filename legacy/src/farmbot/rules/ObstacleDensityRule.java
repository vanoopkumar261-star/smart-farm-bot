package farmbot.rules;

import farmbot.model.BotState;
import farmbot.model.Recommendation;

public class ObstacleDensityRule extends Rule {
    private final int windowSize;

    public ObstacleDensityRule(int windowSize) {
        this.windowSize = windowSize;
    }

    @Override
    public Recommendation evaluate(BotState state) {
        int recentObstacles = state.getObstacleLog().countInLastNMoves(windowSize);
        if (recentObstacles > 3) {
            return new Recommendation("High obstacle density on current row - inspect field");
        }
        return null;
    }
}
