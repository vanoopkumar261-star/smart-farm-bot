package farmbot.concurrency;

import farmbot.data.Reading;
import farmbot.data.SensorRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * The CONSUMER in the producer/consumer pattern, on its own dedicated thread.
 *
 * It blocks on the queue waiting for readings, then drains everything available into
 * one list and batch-inserts it through the repository. Batching means many readings
 * become one transaction, which is far faster than inserting row by row and keeps the
 * single SQLite writer from becoming a bottleneck.
 *
 * Shutdown is lossless: {@link #stop()} sets a flag and the run loop keeps going until
 * the queue is empty, then does a final drain, so no queued reading is lost.
 */
public final class DbWriter implements Runnable {

    private static final long POLL_TIMEOUT_MS = 200;

    private final BlockingQueue<Reading> queue;
    private final SensorRepository repository;
    private final int maxBatch;

    private volatile boolean running = true;
    private final Thread thread = new Thread(this, "db-writer");

    public DbWriter(BlockingQueue<Reading> queue, SensorRepository repository, int maxBatch) {
        this.queue = queue;
        this.repository = repository;
        this.maxBatch = Math.max(1, maxBatch);
    }

    public void start() {
        thread.start();
    }

    @Override
    public void run() {
        // Keep running while asked to, and keep draining whatever is left after a stop.
        while (running || !queue.isEmpty()) {
            try {
                Reading first = queue.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (first == null) {
                    continue; // nothing arrived in this window; check the flag and loop
                }
                List<Reading> batch = new ArrayList<>();
                batch.add(first);
                queue.drainTo(batch, maxBatch - 1); // grab any others already waiting
                repository.insertBatch(batch);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (RuntimeException dbError) {
                // A write failure must not kill the consumer thread.
                System.err.println("[db-writer] batch insert failed: " + dbError);
            }
        }
        flushRemaining();
    }

    /** Final sweep after the loop ends, so a reading queued during shutdown is still stored. */
    private void flushRemaining() {
        List<Reading> remaining = new ArrayList<>();
        queue.drainTo(remaining);
        if (!remaining.isEmpty()) {
            try {
                repository.insertBatch(remaining);
            } catch (RuntimeException dbError) {
                System.err.println("[db-writer] final flush failed: " + dbError);
            }
        }
    }

    /** Signals the thread to finish draining and waits for it to exit. */
    public void stop() {
        running = false;
        try {
            thread.join(TimeUnit.SECONDS.toMillis(3));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
