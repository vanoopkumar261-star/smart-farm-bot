package farmbot.sensor;

import farmbot.data.Reading;
import farmbot.exception.SensorFailureException;
import farmbot.model.SensorType;
import farmbot.model.StatusLabel;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Base class for every sensor on the base platform.
 *
 * read() is a TEMPLATE METHOD: the steps every sensor shares (check connection,
 * take a value, build the Reading) live here once. Each subclass fills in only
 * what is different about it:
 *   - classify(value)  -> the status word for that kind of sensor (ON_LINE, DRY, HALT...)
 *   - formatRaw(value) -> the simulated raw signal (ADC count, echo time...)
 * A subclass may also override read() completely (see CameraSensor).
 *
 * SmartFarmBot keeps a List<Sensor> and calls read() on each one without knowing
 * the concrete type. That is runtime polymorphism.
 */
public abstract class Sensor extends Component {

    private final SensorType type;
    private final boolean critical;
    private final SignalSource source;

    // volatile: the web thread may flip this (fault injection) while a poller thread reads it.
    private volatile boolean connected = true;

    protected Sensor(String id, String name, SensorType type, boolean critical, SignalSource source) {
        super(id, name);
        this.type = Objects.requireNonNull(type, "type");
        this.source = Objects.requireNonNull(source, "source");
        this.critical = critical;
    }

    public Reading read() throws SensorFailureException {
        if (!connected) {
            throw new SensorFailureException(getId(), critical, getName() + " is not responding");
        }
        double value = source.next();
        return new Reading(getId(), type, value, type.getUnit(), formatRaw(value),
                classify(value), LocalDateTime.now());
    }

    /** The status word for a value from this kind of sensor, e.g. DRY or HALT. */
    protected abstract StatusLabel classify(double value);

    /** The simulated raw hardware signal behind the value, stored in the raw column. */
    protected abstract String formatRaw(double value);

    @Override
    public String getCategory() {
        return "Sensor";
    }

    public SensorType getType() {
        return type;
    }

    /** A critical sensor failing halts the bot; a non-critical one only shows a status. */
    public boolean isCritical() {
        return critical;
    }

    public boolean isConnected() {
        return connected;
    }

    /** Simulates unplugging / replugging the sensor (used by tests and the fault demo). */
    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    protected SignalSource getSource() {
        return source;
    }
}
