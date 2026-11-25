package farmbot.concurrency;

import farmbot.core.SmartFarmBot;
import farmbot.data.Reading;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The PRODUCER in the producer/consumer pattern.
 *
 * A ScheduledExecutorService fires {@link #pollOnce()} at a fixed rate (about once a
 * second). Each run asks the bot to read every sensor and ready module, then puts
 * every resulting Reading on a shared BlockingQueue. The DbWriter (the consumer)
 * takes them off the other end on its own thread, so measuring and storing never
 * block each other.
 *
 * The queue is the hand-off point: the producer does not know or care who consumes
 * the readings, only that they are queued. One poll failing is caught here so it can
 * never kill the scheduler and stop all future polls.
 */
public final class SensorPoller {

    private final SmartFarmBot bot;
    private final BlockingQueue<Reading> queue;
    private final long periodMillis;
    private final ScheduledExecutorService scheduler;

    public SensorPoller(SmartFarmBot bot, BlockingQueue<Reading> queue, long periodMillis) {
        this.bot = bot;
        this.queue = queue;
        this.periodMillis = periodMillis;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread t = new Thread(runnable, "sensor-poller");
            t.setDaemon(true);
            return t;
        });
    }

    /** Begins polling immediately and then every {@code periodMillis}. */
    public void start() {
        scheduler.scheduleAtFixedRate(this::pollOnce, 0, periodMillis, TimeUnit.MILLISECONDS);
    }

    /** One poll cycle: read everything, queue every reading. */
    private void pollOnce() {
        try {
            List<Reading> readings = bot.pollSensorsAndModules();
            for (Reading reading : readings) {
                // offer() never blocks: an unbounded queue always accepts, and if the
                // queue were bounded and full we would rather drop one live sample than
                // stall the poller.
                queue.offer(reading);
            }
        } catch (RuntimeException e) {
            System.err.println("[poller] poll failed and was skipped: " + e);
        }
    }

    /** Stops the scheduler and waits briefly for the in-flight poll to finish. */
    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
