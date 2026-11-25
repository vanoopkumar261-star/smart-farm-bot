package farmbot.rules;

/** How urgent a recommendation is. Declared in increasing order, so CRITICAL sorts last (reversed = first). */
public enum Severity {
    INFO,
    WARNING,
    CRITICAL
}
