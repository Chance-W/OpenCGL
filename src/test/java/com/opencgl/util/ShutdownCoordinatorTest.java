package com.opencgl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ShutdownCoordinatorTest {
    @Test
    void cleanupFailureHaltsOnlyOnce() {
        var scheduler = Executors.newSingleThreadScheduledExecutor();
        var halted = new AtomicInteger();
        var normal = new AtomicInteger();
        try {
            var coordinator = new ShutdownCoordinator(scheduler, Runnable::run,
                () -> { throw new IllegalStateException("broken plugin"); },
                normal::incrementAndGet, halted::incrementAndGet, Duration.ofSeconds(2));
            coordinator.shutdown();
            coordinator.shutdown();
            assertEquals(1, halted.get());
            assertEquals(0, normal.get());
        } finally { scheduler.shutdownNow(); }
    }

    @Test
    void rejectedCleanupStillHalts() {
        var scheduler = Executors.newSingleThreadScheduledExecutor();
        var halted = new AtomicInteger();
        try {
            new ShutdownCoordinator(scheduler,
                task -> { throw new java.util.concurrent.RejectedExecutionException(); },
                () -> {}, () -> {}, halted::incrementAndGet, Duration.ofSeconds(2)).shutdown();
            assertEquals(1, halted.get());
            assertTrue(scheduler.isShutdown());
        } finally { scheduler.shutdownNow(); }
    }
    @Test
    void blockedNormalExitStillTriggersWatchdog() throws Exception {
        var scheduler = Executors.newSingleThreadScheduledExecutor();
        var worker = Executors.newSingleThreadExecutor();
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var halted = new CountDownLatch(1);
        try {
            var coordinator = new ShutdownCoordinator(scheduler, worker, () -> {}, () -> {
                entered.countDown();
                await(release);
            }, halted::countDown, Duration.ofMillis(200));
            worker.execute(coordinator::shutdown);
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            assertTrue(halted.await(2, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            worker.shutdownNow();
            scheduler.shutdownNow();
        }
    }

    @Test
    void timedOutCleanupDoesNotExitNormallyAfterReturning() throws Exception {
        var scheduler = Executors.newSingleThreadScheduledExecutor();
        var worker = Executors.newSingleThreadExecutor();
        var release = new CountDownLatch(1);
        var halted = new CountDownLatch(1);
        var normal = new AtomicInteger();
        try {
            var coordinator = new ShutdownCoordinator(scheduler, worker, () -> await(release),
                normal::incrementAndGet, halted::countDown, Duration.ofMillis(100));
            coordinator.shutdown();
            coordinator.shutdown();
            assertTrue(halted.await(2, TimeUnit.SECONDS));
            release.countDown();
            worker.shutdown();
            assertTrue(worker.awaitTermination(2, TimeUnit.SECONDS));
            assertEquals(0, normal.get());
        } finally {
            release.countDown();
            worker.shutdownNow();
            scheduler.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try { latch.await(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    @Test
    void completedCleanupCancelsForcedHaltAndExitsOnce() throws Exception {
        AtomicInteger normalExits = new AtomicInteger();
        AtomicInteger forcedHalts = new AtomicInteger();
        var scheduler = Executors.newSingleThreadScheduledExecutor();
        try {
            ShutdownCoordinator coordinator = new ShutdownCoordinator(
                scheduler,
                Runnable::run,
                () -> {},
                normalExits::incrementAndGet,
                forcedHalts::incrementAndGet,
                Duration.ofMillis(30)
            );

            coordinator.shutdown();
            coordinator.shutdown();
            Thread.sleep(80);

            assertEquals(1, normalExits.get());
            assertEquals(0, forcedHalts.get());
        }
        finally {
            scheduler.shutdownNow();
        }
    }
}
