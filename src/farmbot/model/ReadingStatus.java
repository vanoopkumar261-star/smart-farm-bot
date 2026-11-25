package farmbot.model;

/**
 * Generic SEVERITY level of a reading. The dashboard colours by this.
 * The exact word shown to the user (ON_LINE, DRY, HALT...) is a StatusLabel,
 * and every StatusLabel maps to one of these levels.
 */
public enum ReadingStatus {
    OK,        // normal value
    WARNING,   // outside the comfortable range (e.g. line lost, moisture low)
    CRITICAL,  // must act now (e.g. obstacle very close, battery empty)
    INACTIVE,  // the source is fitted but switched off (e.g. camera)
    ERROR      // the read failed
}
