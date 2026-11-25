package farmbot.sensor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * SIMULATED data source used in place of real hardware.
 *
 * Two modes:
 *  1. Replay: plays back a scripted list of values (the seeded 09:00-09:19 story),
 *     holding each value for {@code samplesPerStep} calls, so one seeded minute
 *     lasts a few seconds of the live demo.
 *  2. Random walk: once the script is used up, it continues from the last value
 *     with a small random step (noise) plus an optional steady drift (e.g.
 *     battery drain), kept inside [min, max].
 *
 * The methods are synchronized because a poller thread calls next() while the
 * main thread may call replay() at start-up.
 */
public class SimulatedSignal implements SignalSource {

    private final double noise;
    private final double min;
    private final double max;
    private final Random random;
    private final List<Double> script = new ArrayList<>();

    private double drift;
    private double current;
    private int samplesPerStep = 1;
    private int sampleIndex;

    public SimulatedSignal(double start, double noise, double min, double max) {
        this(start, noise, min, max, new Random());
    }

    /** Pass a Random with a fixed seed to get repeatable values in tests. */
    public SimulatedSignal(double start, double noise, double min, double max, Random random) {
        if (min >= max) {
            throw new IllegalArgumentException("min must be less than max");
        }
        if (noise < 0) {
            throw new IllegalArgumentException("noise cannot be negative");
        }
        this.noise = noise;
        this.min = min;
        this.max = max;
        this.random = random;
        this.current = clamp(start);
    }

    /** Steady change added on every sample in random-walk mode (negative = falling). */
    public synchronized void setDrift(double drift) {
        this.drift = drift;
    }

    /** Queue a scripted list of values to replay before the random walk takes over. */
    public synchronized void replay(List<Double> values, int samplesPerStep) {
        if (samplesPerStep < 1) {
            throw new IllegalArgumentException("samplesPerStep must be at least 1");
        }
        script.clear();
        script.addAll(values);
        this.samplesPerStep = samplesPerStep;
        this.sampleIndex = 0;
    }

    /** Continue the random walk from this value (e.g. the last seeded reading). */
    public synchronized void setCurrent(double value) {
        this.current = clamp(value);
    }

    public synchronized boolean isReplaying() {
        return sampleIndex < script.size() * samplesPerStep;
    }

    /** The most recent value, without advancing the signal. */
    public synchronized double peek() {
        return current;
    }

    @Override
    public synchronized double next() {
        if (isReplaying()) {
            current = script.get(sampleIndex / samplesPerStep);
            sampleIndex++;
            return current;
        }
        current = clamp(current + drift + random.nextGaussian() * noise);
        return Math.round(current * 100.0) / 100.0;
    }

    private double clamp(double value) {
        return Math.max(min, Math.min(max, value));
    }
}
