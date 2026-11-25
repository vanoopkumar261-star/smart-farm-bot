package farmbot.sensor;

/**
 * Where a sensor gets its raw number from.
 *
 * This is the seam between the software and the (missing) hardware. Today every
 * sensor is given a SimulatedSignal; a real rover would be given a class that
 * reads a serial port or GPIO pin instead, and no Sensor class would change.
 *
 * It has exactly one abstract method, so it is a functional interface and can
 * be written as a lambda, e.g. {@code () -> 0.0}.
 */
@FunctionalInterface
public interface SignalSource {

    /** Returns the next raw value. */
    double next();
}
