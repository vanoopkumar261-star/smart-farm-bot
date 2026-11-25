package farmbot.model;

/**
 * Every kind of data source the rover can report. Each constant carries its own
 * unit and label, so no other class needs a switch statement to look them up.
 *
 * MODULE is shared by all mission modules (sprayer, seed analysis, 3D scanner...),
 * so adding a new module never requires a new enum constant.
 */
public enum SensorType {
    LINE("Line Sensor", "bits"),
    OBSTACLE("Obstacle Sensor", "cm"),
    SOIL_MOISTURE("Soil Moisture", "%"),
    SOIL_TEMPERATURE("Soil Temperature", "C"),
    BATTERY("Battery", "%"),
    CAMERA("Camera", ""),
    MODULE("Mission Module", "");

    private final String displayName;
    private final String unit;

    SensorType(String displayName, String unit) {
        this.displayName = displayName;
        this.unit = unit;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getUnit() {
        return unit;
    }
}
