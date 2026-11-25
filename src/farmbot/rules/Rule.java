package farmbot.rules;

import java.util.Optional;

/**
 * Base class for every advisory rule.
 *
 * RecommendationEngine keeps a List<Rule> and calls evaluate() on each one
 * polymorphically. A new rule is one new subclass, added to the list.
 *
 * evaluate() returns Optional.empty() when there is nothing to advise, so
 * callers never have to check for null.
 */
public abstract class Rule {

    private final String id;
    private final String name;

    protected Rule(String id, String name) {
        if (id == null || id.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Rule id and name are required");
        }
        this.id = id;
        this.name = name;
    }

    public abstract Optional<Recommendation> evaluate(RuleContext context);

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /** Helper so every rule builds its Recommendation the same way, tagged with its own id. */
    protected Recommendation recommend(Severity severity, String title, String message, String action) {
        return new Recommendation(id, severity, title, message, action);
    }
}
