package farmbot.core;

import farmbot.data.Reading;
import farmbot.exception.LineLostException;
import farmbot.exception.ModuleNotReadyException;
import farmbot.exception.SensorFailureException;
import farmbot.model.BotStatus;
import farmbot.model.Direction;
import farmbot.model.SensorType;
import farmbot.model.StatusLabel;
import farmbot.module.MissionModule;
import farmbot.module.Scan3DModule;
import farmbot.module.SeedAnalysisModule;
import farmbot.module.SprayerModule;
import farmbot.nav.FollowLineStrategy;
import farmbot.nav.NavigationStrategy;
import farmbot.nav.SearchStrategy;
import farmbot.sensor.BatterySensor;
import farmbot.sensor.CameraSensor;
import farmbot.sensor.LineSensor;
import farmbot.sensor.ObstacleSensor;
import farmbot.sensor.Sensor;
import farmbot.sensor.SimulatedSignal;
import farmbot.sensor.SoilMoistureSensor;
import farmbot.sensor.SoilTemperatureSensor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * The COMPOSITION ROOT of the rover. It HAS (composition, not inheritance) a
 * drive train, an odometer, a list of sensors and a list of mission modules, and
 * it owns the live snapshot of the latest readings and the current navigation
 * strategy.
 *
 * The key OOP point for the viva: the bot talks to the {@code Sensor},
 * {@code MissionModule} and {@code NavigationStrategy} abstractions only. It reads
 * every sensor and updates every module through those base types with NO instanceof
 * and NO switch on concrete type, so a new sensor or module is one new class and
 * this file never changes (Open/Closed Principle).
 *
 * Thread-safety: a poller thread calls {@link #pollSensorsAndModules()} while a
 * navigation-tick thread calls {@link #tick(double)} and a web thread reads the
 * getters. Shared state is therefore a {@code volatile} status, a {@code volatile}
 * published snapshot map, atomics inside the odometer, and copy-on-write lists.
 */
public final class SmartFarmBot {

    private final String name;
    private final DriveTrain driveTrain;
    private final Odometer odometer;

    private final List<Sensor> sensors = new CopyOnWriteArrayList<>();
    private final List<MissionModule> modules = new CopyOnWriteArrayList<>();

    /** Sensor ids whose failure has already been logged, so the console isn't spammed each poll. */
    private final java.util.Set<String> loggedFaults = ConcurrentHashMap.newKeySet();

    // volatile: written by the control/poller threads, read by the web thread.
    private volatile BotStatus status = BotStatus.IDLE;

    /** The latest reading per sensor type, rebuilt each poll and published atomically. */
    private volatile Map<SensorType, Reading> latest = Map.of();

    // The strategy in use now, and the persistent one to resume after a temporary
    // SearchStrategy finishes. Both volatile: the web thread may swap strategy.
    private volatile NavigationStrategy strategy;
    private volatile NavigationStrategy resumeStrategy;

    public SmartFarmBot(String name, DriveTrain driveTrain, Odometer odometer, NavigationStrategy initialStrategy) {
        this.name = name;
        this.driveTrain = driveTrain;
        this.odometer = odometer;
        this.strategy = initialStrategy;
        this.resumeStrategy = initialStrategy;
    }

    // ---- assembly ---------------------------------------------------------

    public void addSensor(Sensor sensor) {
        sensors.add(sensor);
    }

    /** Mounts a module (running its mount life cycle) and keeps it for polling. */
    public void mountModule(MissionModule module) {
        module.mount();
        modules.add(module);
    }

    // ---- the producer side: one poll cycle --------------------------------

    /**
     * Reads every sensor and every ready module once and returns all the readings.
     * Called by the SensorPoller roughly once a second.
     *
     * The sensors are read with a parallel stream, so several simulated reads happen
     * at the same time; the results are merged into one snapshot that the modules and
     * the navigation tick then use. A failed read never aborts the cycle:
     * {@link #safeRead} turns a SensorFailureException into an empty result (and halts
     * the bot if the failed sensor was critical).
     */
    public List<Reading> pollSensorsAndModules() {
        List<Reading> sensorReadings = sensors.parallelStream()
                .map(this::safeRead)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());

        Map<SensorType, Reading> snapshot = new EnumMap<>(SensorType.class);
        for (Reading reading : sensorReadings) {
            snapshot.put(reading.getSensorType(), reading);
        }
        this.latest = Collections.unmodifiableMap(snapshot); // publish atomically

        List<Reading> all = new ArrayList<>(sensorReadings);
        for (MissionModule module : modules) {
            module.update(this.latest); // let it react (e.g. sprayer switches on)
            if (module.getState().isReady()) {
                try {
                    all.add(module.read());
                } catch (ModuleNotReadyException notReady) {
                    // a stub like the 3D scanner: stays INACTIVE, contributes no reading
                }
            }
        }
        return all;
    }

    /** Reads one sensor, or handles its failure and returns empty. Never throws. */
    private Optional<Reading> safeRead(Sensor sensor) {
        try {
            return Optional.of(sensor.read());
        } catch (SensorFailureException failure) {
            if (failure.isCritical()) {
                status = BotStatus.FAULT;
                driveTrain.cutPower();
            }
            if (loggedFaults.add(sensor.getId())) {
                System.err.println("[bot] sensor " + failure.getSensorId()
                        + (failure.isCritical() ? " CRITICAL" : " (non-critical)")
                        + " failure: " + failure.getMessage());
            }
            return Optional.empty();
        }
    }

    // ---- the control side: one navigation tick ----------------------------

    /**
     * Advances the control loop once, using the latest snapshot. Called by the
     * navigation-tick thread.
     *
     * Order matters: obstacle safety is checked ABOVE the strategy, so no strategy
     * can forget to stop. A LineLostException does not stop the bot; it swaps in a
     * SearchStrategy and keeps going. Any other navigation error (e.g. the Manual
     * strategy's UnsupportedOperationException) is caught and logged, and the finally
     * block cuts motor power on every path that did not successfully drive.
     *
     * @param seconds how long this tick represents, for the odometer
     */
    public void tick(double seconds) {
        if (status == BotStatus.SHUTDOWN || status == BotStatus.FAULT) {
            return; // a critical fault / shutdown keeps the bot stopped
        }
        Map<SensorType, Reading> snap = latest;
        boolean drove = false;
        try {
            Reading obstacle = snap.get(SensorType.OBSTACLE);
            if (obstacle != null && obstacle.getLabel() == StatusLabel.HALT) {
                status = BotStatus.HALTED;
                return;
            }

            NavigationStrategy current = strategy;
            // A finished temporary strategy (search) hands control back to the base one.
            if (current != resumeStrategy && current.isFinished(snap)) {
                current = resumeStrategy;
                current.reset();
                strategy = current;
            }

            Direction direction;
            try {
                direction = current.decide(snap);
                status = BotStatus.RUNNING;
            } catch (LineLostException lost) {
                SearchStrategy search = new SearchStrategy();
                search.reset();
                strategy = search;
                status = BotStatus.SEARCHING;
                System.err.println("[bot] line lost -> switching to SEARCH (" + lost.getMessage() + ")");
                direction = search.decide(snap);
            }

            driveTrain.apply(direction);
            odometer.record(driveTrain.getCurrentSpeed(), seconds);
            drove = true;
        } catch (RuntimeException navFault) {
            // e.g. ManualStrategy throws UnsupportedOperationException: log, do not crash.
            System.err.println("[bot] navigation fault, holding position: " + navFault);
        } finally {
            if (!drove) {
                driveTrain.cutPower(); // safety: no valid drive command this tick -> motors off
            }
        }
    }

    /** Stops the bot cleanly: marks it shut down and cuts motor power. */
    public void shutDown() {
        status = BotStatus.SHUTDOWN;
        driveTrain.cutPower();
    }

    // ---- read-only views for the web / demo layers ------------------------

    public String getName() {
        return name;
    }

    public BotStatus getStatus() {
        return status;
    }

    public DriveTrain getDriveTrain() {
        return driveTrain;
    }

    public Odometer getOdometer() {
        return odometer;
    }

    public List<Sensor> getSensors() {
        return Collections.unmodifiableList(sensors);
    }

    public List<MissionModule> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public String getStrategyName() {
        return strategy.getName();
    }

    /** Switches the persistent navigation strategy (POST /api/strategy later uses this). */
    public void setStrategy(NavigationStrategy newStrategy) {
        newStrategy.reset();
        this.strategy = newStrategy;
        this.resumeStrategy = newStrategy;
    }

    /** The latest reading per sensor type at this moment (may be empty before the first poll). */
    public Map<SensorType, Reading> latestSnapshot() {
        return latest;
    }

    // ---- the standard simulated rig ---------------------------------------

    /**
     * Builds the default rover used by the demo and the web server: six base sensors,
     * three mission modules, a differential drive and a line follower.
     *
     * The simulated signals are seeded so the live run reproduces the demo story -
     * soil moisture starts just above 30% and drifts down, so within a few seconds it
     * crosses the threshold and the sprayer switches itself on; the obstacle and line
     * signals briefly dip to trigger a HALT and a line-loss/SEARCH recovery.
     */
    public static SmartFarmBot demoRig() {
        DriveTrain driveTrain = new DriveTrain(new Motor("MOT-L", "Left Drive Motor"),
                                               new Motor("MOT-R", "Right Drive Motor"), 60);
        SmartFarmBot bot = new SmartFarmBot("Smart Farm Bot", driveTrain, new Odometer(), new FollowLineStrategy());

        // All signals use a FIXED-SEED Random so the live demo is deterministic: the
        // soil-drop -> advisory -> sprayer-ACTIVE beat, the HALT and the line-loss happen
        // on the same ticks every run, matching the story Field Analytics shows.

        // Line: mostly on-line (pattern 2 = 0 1 0), with a scripted two-tick loss to show
        // SEARCH, then holds on-line (noise 0 = no drift after the script).
        SimulatedSignal line = new SimulatedSignal(2, 0.0, 0, 7, new Random(11));
        line.replay(List.of(2.0, 2.0, 2.0, 0.0, 0.0, 4.0, 2.0, 2.0), 1);
        bot.addSensor(new LineSensor("LINE-01", line));

        // Obstacle: clear, with a scripted close pass (<20 cm) to show a HALT, then a gentle
        // walk well above the halt threshold (never re-halts in a normal demo).
        SimulatedSignal obstacle = new SimulatedSignal(180, 3.0, 40, 300, new Random(23));
        obstacle.replay(List.of(180.0, 170.0, 15.0, 15.0, 160.0, 175.0), 1);
        bot.addSensor(new ObstacleSensor("OBS-01", obstacle));

        // Soil moisture: starts at 31%, drifts down ~0.4%/poll, crosses 30% -> DRY -> advisory
        // + sprayer ACTIVE, then settles in the 22-24% DRY band (clamped min 22, so it stays
        // DRY and the advisory holds all through the demo, without bottoming out at 0).
        SimulatedSignal moisture = new SimulatedSignal(31, 0.1, 22, 60, new Random(5));
        moisture.setDrift(-0.4);
        bot.addSensor(new SoilMoistureSensor("SOIL-01", moisture));

        SimulatedSignal soilTemp = new SimulatedSignal(24, 0.1, 10, 40, new Random(31));
        soilTemp.setDrift(0.03);
        bot.addSensor(new SoilTemperatureSensor("TEMP-01", soilTemp));

        SimulatedSignal battery = new SimulatedSignal(80, 0.04, 40, 100, new Random(47));
        battery.setDrift(-0.05);
        bot.addSensor(new BatterySensor("BAT-01", battery));

        bot.addSensor(new CameraSensor("CAM-01")); // always INACTIVE (caught, non-critical)

        bot.mountModule(new SprayerModule("SPRAY-01"));
        bot.mountModule(new SeedAnalysisModule("SEED-01"));
        bot.mountModule(new Scan3DModule("SCAN3D-01")); // stub, stays INACTIVE

        return bot;
    }
}
