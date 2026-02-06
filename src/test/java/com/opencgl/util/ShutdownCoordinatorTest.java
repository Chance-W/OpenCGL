package com.opencgl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ShutdownCoordinatorTest {

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
