package com.opencgl.util;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Keeps the watchdog alive through cleanup AND the exit callback. */
public final class ShutdownCoordinator {
    private final ScheduledExecutorService scheduler;
    private final Executor cleanupExecutor;
    private final Runnable cleanup;
    private final Runnable normalExit;
    private final Runnable forcedHalt;
    private final Duration timeout;
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean finished = new AtomicBoolean();

    public ShutdownCoordinator(
        ScheduledExecutorService scheduler,
        Executor cleanupExecutor,
        Runnable cleanup,
        Runnable normalExit,
        Runnable forcedHalt,
        Duration timeout
    ) {
        this.scheduler = Objects.requireNonNull(scheduler);
        this.cleanupExecutor = Objects.requireNonNull(cleanupExecutor);
        this.cleanup = Objects.requireNonNull(cleanup);
        this.normalExit = Objects.requireNonNull(normalExit);
        this.forcedHalt = Objects.requireNonNull(forcedHalt);
        this.timeout = Objects.requireNonNull(timeout);
    }

    public void shutdown() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        ScheduledFuture<?> timeoutTask = scheduler.schedule(
            this::haltOnce, timeout.toMillis(), TimeUnit.MILLISECONDS
        );
        try {
            cleanupExecutor.execute(() -> {
                try {
                    cleanup.run();
                    if (!finished.get()) normalExit.run();
                    finished.set(true);
                } catch (Throwable failure) {
                    haltOnce();
                } finally {
                    timeoutTask.cancel(false);
                    scheduler.shutdown();
                }
            });
        } catch (RuntimeException rejected) {
            haltOnce();
            timeoutTask.cancel(false);
            scheduler.shutdown();
        }
    }

    private void haltOnce() {
        if (finished.compareAndSet(false, true)) forcedHalt.run();
    }
}
