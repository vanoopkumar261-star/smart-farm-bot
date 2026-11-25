package farmbot.concurrency;

import farmbot.core.SmartFarmBot;
import farmbot.data.Reading;
import farmbot.data.SensorRepository;
import farmbot.model.BotStatus;
import farmbot.model.SensorType;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wires the live engine together and owns its three threads:
 *   1. the SensorPoller  - produces readings onto the shared queue;
 *   2. the DbWriter       - consumes the queue and batch-writes to SQLite;
 *   3. a navigation ticker - advances the control loop (follow / search / halt).
 *
 * It also registers a JVM shutdown hook so that Ctrl-C (or the web server stopping)
 * always shuts the engine down cleanly: stop producing, let the consumer flush the
 * queue, cut motor power, and close the database connection. The whole stop sequence
 * is guarded by an AtomicBoolean so it runs exactly once even if both the hook and an
 * explicit stop() fire.
 */
public final class FarmBotRuntime {

    private final SmartFarmBot bot;
    private final SensorRepository repository;

    private final LinkedBlockingQueue<Reading> queue = new LinkedBlockingQueue<>();
    private final SensorPoller poller;
    private final DbWriter writer;
    private final ScheduledExecutorService navTicker;

    private final double tickSeconds;
    private final long tickMillis;
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    // Last status seen by the ticker, so an event is logged once per transition, not every tick.
    private volatile BotStatus lastStatus = BotStatus.IDLE;

    public FarmBotRuntime(SmartFarmBot bot, SensorRepository repository, long pollMillis, long tickMillis) {
        this.bot = bot;
        this.repository = repository;
        this.tickMillis = tickMillis;
        this.tickSeconds = tickMillis / 1000.0;
        this.poller = new SensorPoller(bot, queue, pollMillis);
        this.writer = new DbWriter(queue, repository, 64);
        this.navTicker = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread t = new Thread(runnable, "nav-ticker");
            t.setDaemon(true);
            return t;
        });
    }

    /** Starts the consumer first, then the producer and the control loop. */
    public void start() {
        writer.start();
        poller.start();
        // The two standing faults the rover always has: the camera is not fitted and the
        // 3D scanner is a stub. Recording them means the Faults panel is never empty.
        repository.recordFault("CAM-01", "ConnectException", "ESP32-CAM not connected - feed INACTIVE");
        repository.recordFault("SCAN3D-01", "ModuleNotReadyException", "3D scanner mounted but not ready - stub module");
        navTicker.scheduleAtFixedRate(this::safeTick, tickMillis, tickMillis, TimeUnit.MILLISECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "shutdown-hook"));
        System.out.println("[runtime] engine started (poller + db-writer + nav-ticker)");
    }

    private void safeTick() {
        try {
            bot.tick(tickSeconds);
            recordTransition();
        } catch (RuntimeException e) {
            System.err.println("[runtime] tick failed and was skipped: " + e);
        }
    }

    /** Logs an obstacle event or a line-loss fault once, when the status first changes to it. */
    private void recordTransition() {
        BotStatus now = bot.getStatus();
        if (now == lastStatus) {
            return;
        }
        lastStatus = now;
        if (now == BotStatus.HALTED) {
            Reading obstacle = bot.latestSnapshot().get(SensorType.OBSTACLE);
            repository.recordObstacleEvent(obstacle == null ? null : obstacle.getValue(), "HALTED");
        } else if (now == BotStatus.SEARCHING) {
            repository.recordFault("LINE-01", "LineLostException",
                    "All IR channels read 0 - switched to SearchStrategy");
        }
    }

    /**
     * Clean shutdown, in order: stop the control loop and the poller (no new readings),
     * drain the queue through the writer, cut motor power, and close the database.
     * Safe to call more than once.
     */
    public void stop() {
        if (!stopped.compareAndSet(false, true)) {
            return;
        }
        System.out.println("[runtime] shutting down...");
        navTicker.shutdown();
        try {
            navTicker.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        poller.stop();   // stop producing
        writer.stop();   // drain and flush everything still queued
        bot.shutDown();  // cut motor power, mark SHUTDOWN
        repository.close();
        System.out.println("[runtime] shutdown complete");
    }
}
