package com.opencgl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class AsyncUiPipelineTest {

    @Test
    void completionWaitsUntilUiRenderingHasFinished() throws Exception {
        List<String> events = new ArrayList<>();
        AtomicReference<Runnable> queuedUiWork = new AtomicReference<>();
        Executor queuedUiExecutor = queuedUiWork::set;

        CompletableFuture<Void> completion = AsyncUiPipeline.prepareThenRender(
            () -> {
                events.add("prepared");
                return "plugin-view";
            },
            view -> events.add("rendered:" + view),
            Runnable::run,
            queuedUiExecutor);

        assertEquals(List.of("prepared"), events);
        assertFalse(completion.isDone(), "loading must remain active while UI work is only queued");

        queuedUiWork.get().run();

        completion.get(1, TimeUnit.SECONDS);
        assertEquals(List.of("prepared", "rendered:plugin-view"), events);
    }
}
