package com.liftoff.dev;

import com.liftoff.app.LiftoffApp;
import com.liftoff.data.CatalogLoader;
import com.liftoff.model.Catalog;
import com.liftoff.session.GameSession;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Times what the player feels: start-up to the first frame of the title screen, opening the
 * builder, a part swap, and flying a mission. Run it before and after a change and compare.
 * Writes a mission log under the user's home folder only if the real app does; it does not fly
 * through the interface.
 */
public final class PerfProbe {

    private static final List<String> REPORT = new ArrayList<>();
    private static final long T0 = System.nanoTime();

    private PerfProbe() {
    }

    public static void main(String[] args) throws Exception {
        CompletableFuture<Void> started = new CompletableFuture<>();
        Platform.startup(() -> started.complete(null));
        started.get();
        Platform.setImplicitExit(false);
        record("JavaFX toolkit up", ms(T0));

        CompletableFuture<Void> done = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                startApp(done);
            } catch (Throwable problem) {
                done.completeExceptionally(problem);
            }
        });
        done.get(5, TimeUnit.MINUTES);
        Platform.exit();

        measureEngine();
        REPORT.forEach(System.out::println);
    }

    private static void startApp(CompletableFuture<Void> done) throws Exception {
        long before = System.nanoTime();
        Stage stage = new Stage();
        new LiftoffApp().start(stage);
        record("LiftoffApp.start() on the FX thread", ms(before));
        afterFrame(() -> {
            record("process start -> first title frame", ms(T0));
            record("start() call -> first title frame", ms(before));
            Scene scene = stage.getScene();
            whenStartable(scene, () -> {
                record("process start -> Start button usable", ms(T0));
                titleOnwards(scene, stage, done);
            });
        });
    }

    /** The title can come up before the game data has loaded; its Start button says when it has. */
    private static void whenStartable(Scene scene, Runnable then) {
        new AnimationTimer() {
            @Override
            public void handle(long now) {
                scene.getRoot().applyCss();
                Node start = scene.lookup(".primary");
                if (start != null && !start.isDisabled()) {
                    stop();
                    then.run();
                }
            }
        }.start();
    }

    private static void titleOnwards(Scene scene, Stage stage, CompletableFuture<Void> done) {
        {
            timed(scene, "Title -> mission select (click to frame)", () -> fire(scene, ".primary"), () ->
                timed(scene, "Mission select -> briefing", () -> fire(scene, "#mission-farm-watch"), () ->
                    readFor(Long.getLong("probe.readMs", 0), () ->
                        timed(scene, "Briefing -> ship builder (click to frame)", () -> fire(scene, ".primary"), () ->
                            swaps(scene, stage, done)))));
        }
    }

    private static final String[][] SWAPS = {
            {"AIRFRAME", "eo1-class", "cold"}, {"AIRFRAME", "landsat7-class", "cold"},
            {"AIRFRAME", "shuttle-orbiter", "cold"}, {"AIRFRAME", "sorce-class", "cold"},
            {"THRUSTERS", "mr-106", "cold"}, {"POWER", "pumpkin-clamshell", "cold"},
            {"CARGO_BAY", "bay-6u", "cold"}, {"SATELLITE", "cubesat-3u", "cold"},
            {"AIRFRAME", "eo1-class", "warm"}, {"AIRFRAME", "landsat7-class", "warm"},
            {"AIRFRAME", "eo1-class", "warm"}, {"SATELLITE", "cubesat-6u", "cold"},
            {"SATELLITE", "cubesat-3u", "warm"}, {"POWER", "sp6x-wings", "cold"},
            {"POWER", "pumpkin-clamshell", "warm"}};

    private static void swaps(Scene scene, Stage stage, CompletableFuture<Void> done) {
        swap(scene, 0, () -> {
            stage.close();
            done.complete(null);
        });
    }

    private static void swap(Scene scene, int index, Runnable then) {
        if (index == SWAPS.length) {
            then.run();
            return;
        }
        String[] swap = SWAPS[index];
        fire(scene, "#slot-" + swap[0]);
        afterFrame(() -> timed(scene, "Part swap " + swap[2] + " " + swap[1],
                () -> fire(scene, "#part-" + swap[1]), () -> swap(scene, index + 1, then)));
    }

    /** Runs the action, records its time on the FX thread and the time to the frame after it. */
    private static void timed(Scene scene, String label, Runnable action, Runnable then) {
        long before = System.nanoTime();
        action.run();
        double onThread = ms(before);
        afterFrame(() -> {
            record(label, onThread, ms(before));
            then.run();
        });
    }

    /** A player reads the briefing before building; -Dprobe.readMs sets for how long. */
    private static void readFor(long millis, Runnable then) {
        if (millis <= 0) {
            then.run();
            return;
        }
        var wait = new javafx.animation.PauseTransition(javafx.util.Duration.millis(millis));
        wait.setOnFinished(event -> then.run());
        wait.play();
    }

    /** Waits for the frame after the current one has been rendered, then runs the action. */
    private static void afterFrame(Runnable action) {
        new AnimationTimer() {
            private int pulses;

            @Override
            public void handle(long now) {
                if (++pulses == 2) {
                    stop();
                    action.run();
                }
            }
        }.start();
    }

    private static void fire(Scene scene, String selector) {
        scene.getRoot().applyCss();
        scene.getRoot().layout();
        Node node = scene.lookup(selector);
        if (node == null) {
            throw new IllegalStateException("Missing " + selector);
        }
        ((ButtonBase) node).fire();
    }

    private static void measureEngine() {
        long before = System.nanoTime();
        Catalog catalog = CatalogLoader.loadFromClasspath();
        record("Catalog load (warm JVM)", ms(before));

        String[][] plans = {
                {"farm-watch", "eo1-class,mr-106,pumpkin-clamshell,bay-6u,cubesat-3u", "ali", "atlas-v-401"},
                {"moon-landing", "apollo-service-module,rl10c-1,shuttle-fuel-cell,apollo-11-adapter,apollo-11-lander",
                        "lola", "saturn-v"},
                {"mars-drones", "curiosity-cruise,mr-106,mmrtg,mars-2020-entry,perseverance",
                        "mastcam-z,supercam", "atlas-v-541"}};
        for (String[] plan : plans) {
            double best = Double.MAX_VALUE;
            double first = 0;
            for (int run = 0; run < 30; run++) {
                GameSession session = new GameSession(catalog);
                session.selectMission(plan[0]);
                session.next();
                for (String part : plan[1].split(",")) {
                    session.choosePart(part);
                }
                session.next();
                for (String instrument : plan[2].split(",")) {
                    session.addInstrument(instrument);
                }
                if (plan[0].equals("farm-watch")) {
                    session.chooseOrbit("sso-705");
                } else if (plan[0].equals("moon-landing")) {
                    session.setCrewSuppliesKg(80);
                }
                session.toggleStation("svalbard");
                session.chooseLaunchVehicle(plan[3]);
                session.next();
                session.confirmBuild();
                session.next();
                session.runLaunchCheck();
                session.next();
                long start = System.nanoTime();
                session.fly(run);
                double took = ms(start);
                if (run == 0) {
                    first = took;
                }
                best = Math.min(best, took);
            }
            record("Fly " + plan[0] + " (first, best of 30)", first, best);
        }
        try {
            var url = PerfProbe.class.getResource("/com/liftoff/models/hangar.png");
            Image image = new Image(url.toExternalForm());
            record("hangar.png size px " + (int) image.getWidth() + "x" + (int) image.getHeight(), 0);
        } catch (RuntimeException ignored) {
            // The toolkit is down by now on some machines; the size is a side note.
        }
    }

    private static double ms(long since) {
        return (System.nanoTime() - since) / 1e6;
    }

    private static void record(String label, double... millis) {
        StringBuilder line = new StringBuilder(String.format(Locale.ROOT, "%-48s", label));
        for (double value : millis) {
            line.append(String.format(Locale.ROOT, "%10.1f ms", value));
        }
        REPORT.add(line.toString());
    }
}
