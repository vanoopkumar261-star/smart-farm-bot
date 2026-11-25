package farmbot.rules;

import farmbot.model.BotState;
import farmbot.model.Recommendation;

public class PathLossRule extends Rule {
    @Override
    public Recommendation evaluate(BotState state) {
        if (state.getLineLostCount() > 2) {
            return new Recommendation("Line repeatedly lost - check path markings");
        }
        return null;
    }
}
