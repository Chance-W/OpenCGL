package com.opencgl.util;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Coordinates work that prepares data away from the UI thread and then renders
 * that data on the UI thread. The returned future completes only after the
 * render step has returned, so callers can safely bind a loading indicator to
 * the complete operation rather than merely to queue submission.
 */
public final class AsyncUiPipeline {

    private AsyncUiPipeline() {
    }

    public static <T> CompletableFuture<Void> prepareThenRender(
        Supplier<T> prepare,
        Consumer<T> render,
        Executor prepareExecutor,
        Executor renderExecutor) {
        Objects.requireNonNull(prepare, "prepare");
        Objects.requireNonNull(render, "render");
        Objects.requireNonNull(prepareExecutor, "prepareExecutor");
        Objects.requireNonNull(renderExecutor, "renderExecutor");

        return CompletableFuture.supplyAsync(prepare, prepareExecutor)
            .thenAcceptAsync(render, renderExecutor);
    }
}
