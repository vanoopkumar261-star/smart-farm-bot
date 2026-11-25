package farmbot.sensor;

/**
 * Root of the hardware hierarchy: anything physically fitted to the rover.
 * Sensors extend it through Sensor; Motor (package core) extends it directly.
 *
 * Abstract because "a component" on its own is not a real thing; every concrete
 * part must say what category it belongs to.
 */
public abstract class Component {

    private final String id;
    private final String name;

    protected Component(String id, String name) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Component id is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Component name is required");
        }
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /** e.g. "Sensor" or "Actuator"; shown on the Module Bay cards. */
    public abstract String getCategory();

    @Override
    public String toString() {
        return getCategory() + " " + id + " (" + name + ")";
    }
}
