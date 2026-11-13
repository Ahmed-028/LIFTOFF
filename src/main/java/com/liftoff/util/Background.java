package com.liftoff.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Threads for work that must not hold up the screen: loading game data, decoding pictures,
 * computing a flight, writing files. All daemons, so they never keep the game from closing.
 */
public final class Background {

    /** Loading, decoding and computing; tasks may run side by side. */
    public static final ExecutorService POOL = Executors.newFixedThreadPool(2, daemons("liftoff-worker"));

    private Background() {
    }

    /** One thread that runs its tasks strictly in order, for writes to the same file. */
    public static ExecutorService serial(String name) {
        return Executors.newSingleThreadExecutor(daemons(name));
    }

    private static ThreadFactory daemons(String name) {
        AtomicInteger count = new AtomicInteger();
        return task -> {
            Thread thread = new Thread(task, name + "-" + count.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
