package com.numb3r3.common.math.stats;

import java.util.concurrent.*;


/**
 * An object that periodically updates a {@link DataAccumulator},
 * swapping between the buffers.
 *
 * @author netlixoss $
 * @version $Revision: $
 */
public class DataPublisher {

    private static final String THREAD_NAME = "DataPublisher";
    private static final boolean DAEMON_THREADS = true;
    private static ScheduledExecutorService sharedExecutor = null;

    private final DataAccumulator accumulator;
    private final long delayMillis;
    private Future<?> future = null;

    /**
     * Creates a new {@code DataPublisher}.
     * When created it is not running; it is up to the caller to call {@link #start}.
     *
     * @param accumulator the DataAccumulator to periodically publish
     * @param delayMillis the number of milliseconds between publish events
     */
    public DataPublisher(DataAccumulator accumulator,
                         long delayMillis) {
        this.accumulator = accumulator;
        this.delayMillis = delayMillis;
    }

    /**
     * Gets the {@code DataAccumulator} that is managed by this publisher.
     */
    public DataAccumulator getDataAccumulator() {
        return accumulator;
    }

    /**
     * Is the {@code DataPublisher} scheduled to run?
     */
    public synchronized boolean isRunning() {
        return (future != null);
    }

    /*
     * Scheduling data publication
     */

    /**
     * Starts the {@code DataPublisher}.
     * The method {@link DataAccumulator#publish} will be called approximately
     * every {@code delayMillis} milliseconds.
     * If the publisher has already been started, does nothing.
     *
     * @see #stop
     */
    public synchronized void start() {
        if (future == null) {
            Runnable task = new Runnable() {
                public void run() {
                    try {
                        accumulator.publish();
                    } catch (Exception e) {
                        handleException(e);
                    }
                }
            };
            future = getExecutor().scheduleWithFixedDelay(task,
                    delayMillis, delayMillis,
                    TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Gets the {@link ScheduledExecutorService} to use to run the task to periodically
     * update the {@code DataAccumulator}.
     * The default uses a global executor pool for all {@code DataPublisher}s.
     * Subclasses are free to override this if desired, for example to use
     * a per-publisher executor pool.
     */
    protected synchronized ScheduledExecutorService getExecutor() {
        if (sharedExecutor == null) {
            sharedExecutor = Executors.newScheduledThreadPool(1, new PublishThreadFactory());
        }
        return sharedExecutor;
    }

    private static final class PublishThreadFactory implements ThreadFactory {
        PublishThreadFactory() {
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, THREAD_NAME);
            t.setDaemon(DAEMON_THREADS);
            return t;
        }
    }

    /**
     * Stops publishing new data.
     *
     * @see #start
     */
    public synchronized void stop() {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
    }

    /**
     * Called if an attempt to publish data throws an exception.
     * The default does nothing.
     * Subclasses are free to override this.
     */
    protected void handleException(Exception e) {
        // Do nothing, for now
    }

} // DataPublisher