package com.opencgl.util;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Runs shutdown cleanup once and cancels the forced-exit timer after success. */
public final class ShutdownCoordinator {
    private final ScheduledExecutorService scheduler;
    private final Executor cleanupExecutor;
    private final Runnable cleanup;
    private final Runnable normalExit;
    private final Runnable forcedHalt;
    private final Duration timeout;
    private final AtomicBoolean started = new AtomicBoolean();

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
            forcedHalt, timeout.toMillis(), TimeUnit.MILLISECONDS
        );
        CompletableFuture.runAsync(cleanup, cleanupExecutor).whenComplete((ignored, failure) -> {
            timeoutTask.cancel(false);
            scheduler.shutdownNow();
            if (failure == null) {
                normalExit.run();
            }
            else {
                forcedHalt.run();
            }
        });
    }
}
