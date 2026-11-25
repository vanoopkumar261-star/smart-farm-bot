package farmbot.rules;

import java.time.LocalDateTime;
import java.util.Objects;

/** One advisory produced by a Rule. Immutable, like Reading. */
public final class Recommendation {

    private final String ruleId;
    private final Severity severity;
    private final String title;
    private final String message;
    private final String action;
    private final LocalDateTime createdAt;

    public Recommendation(String ruleId, Severity severity, String title, String message, String action) {
        this.ruleId = Objects.requireNonNull(ruleId, "ruleId");
        this.severity = Objects.requireNonNull(severity, "severity");
        this.title = Objects.requireNonNull(title, "title");
        this.message = Objects.requireNonNull(message, "message");
        this.action = Objects.requireNonNull(action, "action");
        this.createdAt = LocalDateTime.now();
    }

    public String getRuleId() {
        return ruleId;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getAction() {
        return action;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "[" + severity + "] " + title + ": " + message + " -> " + action;
    }
}
