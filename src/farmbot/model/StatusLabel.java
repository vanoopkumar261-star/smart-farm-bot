package farmbot.model;

import java.util.Locale;

/**
 * The exact status WORD stored with every reading (in the CSV seed files and the
 * status column of sensor_readings), e.g. ON_LINE, LINE_LOST, HALT, DRY, ACTIVE.
 *
 * Each word maps to one generic level (ReadingStatus: OK / WARNING / CRITICAL /
 * INACTIVE / ERROR). The dashboard colours a card by the level but shows the
 * exact word. Keeping the two apart means a new word only needs one new line here,
 * and code that only cares about severity never changes.
 */
public enum StatusLabel {
    // line sensor
    ON_LINE(ReadingStatus.OK),
    DRIFT(ReadingStatus.OK),           // line seen by a side eye; the bot is steering back
    LINE_LOST(ReadingStatus.WARNING),
    // obstacle sensor
    CLEAR(ReadingStatus.OK),
    NEAR(ReadingStatus.WARNING),
    HALT(ReadingStatus.CRITICAL),
    // soil moisture, soil temperature, battery
    NORMAL(ReadingStatus.OK),
    DRY(ReadingStatus.WARNING),
    VERY_DRY(ReadingStatus.CRITICAL),
    COLD(ReadingStatus.WARNING),
    HOT(ReadingStatus.WARNING),
    LOW(ReadingStatus.WARNING),
    EMPTY(ReadingStatus.CRITICAL),
    // mission modules
    IDLE(ReadingStatus.OK),
    ACTIVE(ReadingStatus.OK),
    TANK_LOW(ReadingStatus.WARNING),
    SUITABLE(ReadingStatus.OK),
    POOR(ReadingStatus.WARNING),
    // any source
    INACTIVE(ReadingStatus.INACTIVE),
    ERROR(ReadingStatus.ERROR);

    private final ReadingStatus level;

    StatusLabel(ReadingStatus level) {
        this.level = level;
    }

    public ReadingStatus getLevel() {
        return level;
    }

    /**
     * Parses a status word from a CSV file or the database. Case and surrounding
     * spaces are ignored, and "line lost" or "line-lost" also match LINE_LOST.
     *
     * @throws IllegalArgumentException for an unknown word; SeedLoader treats that
     *         row as malformed, logs it and skips it
     */
    public static StatusLabel fromText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Status word is missing");
        }
        String normalised = text.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        try {
            return valueOf(normalised);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown status word '" + text.trim() + "'", e);
        }
    }
}
