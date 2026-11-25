package farmbot.rules;

import farmbot.model.BotState;
import farmbot.model.Recommendation;

public abstract class Rule {
    public abstract Recommendation evaluate(BotState state);
}
