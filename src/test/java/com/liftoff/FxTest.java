package com.liftoff;

import javafx.application.Platform;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs test code on the JavaFX thread. Screens are built and driven without ever showing a
 * window. If this machine cannot start JavaFX at all (no display), the calling test is skipped
 * rather than failed, and says so.
 */
public final class FxTest {

    private static Boolean available;

    private FxTest() {
    }

    @FunctionalInterface
    public interface Action {
        void run() throws Exception;
    }

    private static synchronized boolean start() {
        if (available == null) {
            try {
                CompletableFuture<Void> started = new CompletableFuture<>();
                Platform.startup(() -> started.complete(null));
                started.get(30, TimeUnit.SECONDS);
                Platform.setImplicitExit(false);
                available = true;
            } catch (IllegalStateException alreadyRunning) {
                available = true;
            } catch (Exception | Error noToolkit) {
                available = false;
            }
        }
        return available;
    }

    /**
     * Lets everything the last action queued on the FX thread run (screens redraw once per pulse,
     * not once per change), so the next {@link #run} sees the screen as the player would.
     */
    public static void settle() {
        run(() -> { });
    }

    /** Settles until the condition, checked on the FX thread, holds; fails after 10 seconds. */
    public static void await(java.util.function.BooleanSupplier condition, String what) {
        long end = System.nanoTime() + 10_000_000_000L;
        boolean[] met = {false};
        while (System.nanoTime() < end) {
            run(() -> met[0] = condition.getAsBoolean());
            if (met[0]) {
                return;
            }
            Thread.onSpinWait();
        }
        throw new AssertionError("Timed out waiting for " + what);
    }

    /** Runs the action on the JavaFX thread and rethrows whatever it throws. */
    public static void run(Action action) {
        assumeTrue(start(), "JavaFX cannot start on this machine, so this screen test did not run");
        CompletableFuture<Void> done = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                action.run();
                done.complete(null);
            } catch (Throwable problem) {
                done.completeExceptionally(problem);
            }
        });
        try {
            done.get(120, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Error error) {
                throw error;
            }
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new AssertionError(e.getCause());
        } catch (InterruptedException | TimeoutException e) {
            throw new AssertionError("The JavaFX thread did not finish", e);
        }
    }
}
