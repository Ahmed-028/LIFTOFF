package com.liftoff.dev;

import com.liftoff.app.LiftoffApp;
import com.liftoff.app.SceneNavigator;
import com.liftoff.app.Screen;
import com.liftoff.data.CatalogLoader;
import com.liftoff.model.LaunchVehicle;
import com.liftoff.model.Orbit;
import com.liftoff.session.GameSession;
import com.liftoff.session.MissionStep;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Clicks through the shipped UI and captures each screen at both supported QA sizes. */
public final class SnapshotTool {
    private final Path folder;
    private final CompletableFuture<Void> done = new CompletableFuture<>();
    private Path current;

    private SnapshotTool(Path folder) {
        this.folder = folder;
    }

    public static void main(String[] args) throws Exception {
        Path folder = Path.of(args.length > 0 ? args[0] : "target/snapshots");
        System.setProperty("liftoff.sound", "off");
        var tool = new SnapshotTool(folder);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            LiftoffApp.loadFonts();
            tool.start(800);
        });
        try {
            tool.done.get(150, TimeUnit.SECONDS);
        } finally {
            Platform.exit();
        }
        System.out.println("Snapshots written to " + folder.toAbsolutePath());
    }

    private void start(int height) {
        verify(() -> {
            var session = new GameSession(CatalogLoader.loadFromClasspath());
            var scene = new Scene(new StackPane(), 1280, height);
            scene.getStylesheets().add(LiftoffApp.class.getResource(LiftoffApp.STYLESHEET).toExternalForm());
            var messages = ResourceBundle.getBundle(LiftoffApp.MESSAGES_BUNDLE);
            var navigator = new SceneNavigator(scene, session, messages);
            if (height == 800) {
                navigator.setScreenDecorator(new com.liftoff.ui.NovaGuide(navigator, session, messages)::decorate);
            }
            var stage = new Stage();
            stage.setScene(scene);
            stage.show();

            var output = folder.resolve("1280x" + height);
            Files.createDirectories(output);
            current = output;
            if (height == 800) {
                save(new Scene(new StackPane(new javafx.scene.image.ImageView(LiftoffApp.icon(256))), 256, 256,
                        javafx.scene.paint.Color.TRANSPARENT), folder.resolve("icon.png"));
                navigator.playIntro();
                after(1500, () -> {
                    save(scene, output.resolve("00-intro.png"));
                    button(scene, "#skip-cutscene").fire();
                    after(4800, () -> {
                        save(scene, output.resolve("01-title.png"));
                        playThrough(scene, session, stage, output, height);
                    });
                });
            } else {
                navigator.show(Screen.TITLE);
                save(scene, output.resolve("01-title.png"));
                playThrough(scene, session, stage, output, height);
            }
        });
    }

    private void playThrough(Scene scene, GameSession session, Stage stage, Path output, int height) {
        verify(() -> {
            button(scene, ".primary").fire();
            save(scene, output.resolve("02-mission-select.png"));
            button(scene, "#open-sources").fire();
            save(scene, output.resolve("10-sources.png"));
            var sources = (TitledPane) lookup(scene, ".titled-pane");
            sources.setAnimated(false);
            sources.setExpanded(true);
            require(lookup(scene, ".hyperlink") != null, "Source citations are missing");
            button(scene, ".secondary").fire();

            button(scene, "#mission-farm-watch").fire();
            require(lookup(scene, "#skip-cutscene") != null, "Choosing a mission did not play its cutscene");
            button(scene, "#skip-cutscene").fire();
            require(session.step() == MissionStep.BRIEFING, "Mission card did not start at the briefing");
            require(lookup(scene, "#play-level") != null, "Mission card did not open the road map");
            save(scene, output.resolve("02b-road-map.png"));
            button(scene, "#level-pack").fire();
            require(lookup(scene, "#play-level") != null, "A locked level opened");
            button(scene, "#play-level").fire();
            save(scene, output.resolve("03-briefing.png"));
            button(scene, ".primary").fire();
            require(session.step() == MissionStep.PLAN, "The briefing did not lead to the flight plan");
            var orbit = (ChoiceBox<?>) lookup(scene, "#orbit");
            orbit.getSelectionModel().select(orbitIndex(orbit, "sso-705"));
            chooseRocket(scene, "electron");
            save(scene, output.resolve("03b-flight-plan.png"));
            button(scene, "#toShip").fire();
            require(session.step() == MissionStep.SHIP, "The flight plan did not lead to the ship builder");
            require(button(scene, "#supplies").isDisabled(), "An empty build can reach Supplies");
            var confirm = (javafx.scene.control.Button) button(scene, "#confirm");
            require(confirm.isDisabled() && confirm.getTooltip().getText().contains("Cargo bay"),
                    "The disabled confirmation does not explain missing parts");

            var airframe = button(scene, "#part-eo1-class");
            Event.fireEvent(airframe, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER,
                    false, false, false, false));
            var airframeTab = lookup(scene, "#slot-AIRFRAME");
            Event.fireEvent(airframeTab, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.DOWN,
                    false, false, false, false));
            require(lookup(scene, "#part-mr-106") != null, "Arrow key did not switch module tabs");
            part(scene, "THRUSTERS", "mr-106");
            part(scene, "POWER", "pumpkin-clamshell");
            part(scene, "CARGO_BAY", "bay-6u");
            part(scene, "SATELLITE", "cubesat-3u");
            require(session.build().isComplete(), "Five selected cards did not complete the ship");

            after(300, () -> {
                save(scene, output.resolve("04-ship-builder.png"));
                button(scene, "#supplies").fire();
                require(lookup(scene, "#play-level") != null, "Finishing the ship did not fly to the road map");
                save(scene, output.resolve("04b-road-map-travel.png"));
                button(scene, "#play-level").fire();
                require(lookup(scene, "#skip-game") != null, "Level 2 did not open Star Match");
                save(scene, output.resolve("04c-star-match.png"));
                button(scene, "#skip-game").fire();
                require(session.levels().isDone(com.liftoff.session.Level.STAR_MATCH), "Skipping did not finish Star Match");
                button(scene, "#play-level").fire();
                require(session.step() == MissionStep.SUPPLIES, "Supplies step did not open");
                supplyTab(scene, 0);
                button(scene, "#instrument-ali").fire();
                button(scene, "#details-ali").fire();
                save(scene, output.resolve("11-instrument-preview.png"));
                supplyTab(scene, 1);
                button(scene, "#station-svalbard").fire();
                button(scene, "#station-fairbanks").fire();
                var fixMass = (javafx.scene.control.Button) lookup(scene, "#fix-mass");
                require(fixMass != null, "The over-limit mass gauge offers no tips");
                fixMass.fire();
                var tips = javafx.stage.Window.getWindows().stream()
                        .filter(window -> window instanceof javafx.stage.Popup && window.isShowing()).findFirst();
                require(tips.isPresent(), "The tips did not open");
                save(tips.get().getScene(), output.resolve("05b-limit-tips.png"));
                tips.get().hide();
                button(scene, "#open-settings").fire();
                var settings = javafx.stage.Window.getWindows().stream()
                        .filter(window -> window instanceof javafx.stage.Popup && window.isShowing()).findFirst();
                require(settings.isPresent(), "The settings menu did not open");
                save(settings.get().getScene(), output.resolve("05c-settings.png"));
                settings.get().hide();
                button(scene, "#review").fire();
                button(scene, "#confirm").fire();
                require(lookup(scene, "#play-level") != null, "Confirming the build did not fly to the road map");
                button(scene, "#play-level").fire();
                require(lookup(scene, "#skip-game") != null, "Level 4 did not open Asteroid Dash");
                if (height == 800) {
                    button(scene, "#start-ride").fire();
                }
                after(height == 800 ? 1600 : 0, () -> {
                save(scene, output.resolve("07b-asteroid-dash.png"));
                button(scene, "#skip-game").fire();
                button(scene, "#play-level").fire();
                require(session.step() == MissionStep.LAUNCH, "Level 5 did not open the launch");
                button(scene, "#check").fire();
                require(session.launchIssues().stream().anyMatch(issue ->
                                issue.kind() == com.liftoff.engine.sim.LaunchIssue.Kind.MASS_OVER_CAPACITY),
                        "Launch did not show the inadequate rocket warning");
                require(lookup(scene, ".warning-action") != null, "Launch warning has no next action");
                for (int back = 0; back < 4; back++) {
                    button(scene, ".action-bar .secondary").fire();
                }
                require(session.step() == MissionStep.PLAN, "Going back did not reach the flight plan");
                chooseRocket(scene, "atlas-v-401");
                button(scene, "#toShip").fire();
                button(scene, "#supplies").fire();
                toMissionLevel(scene);
                save(scene, output.resolve("05-supplies.png"));
                button(scene, "#review").fire();
                save(scene, output.resolve("06-review.png"));
                require(!button(scene, "#confirm").isDisabled(), "Valid build has issues: " + session.buildReport());
                button(scene, "#confirm").fire();
                toMissionLevel(scene);
                save(scene, output.resolve("07-launch.png"));
                verifyCabin(scene, "#launch-views");
                save(scene, output.resolve("14-launch-cabin.png"));
                button(scene, "#check").fire();
                require(session.launchIssues().isEmpty(), "Launch failed: " + session.launchIssues());
                after(3200, () -> {
                    require(session.step() == MissionStep.FLY, "Countdown did not open flight");
                    require(lookup(scene, "#flight-phase") != null, "Flight has no phase caption");
                    var views = verifyCabin(scene, "#flight-views");
                    save(scene, output.resolve("15-flight-cabin.png"));
                    views.getSelectionModel().selectFirst();
                    button(scene, "#skip-playback").fire();
                    save(scene, output.resolve("08-fly.png"));
                    button(scene, "#report").fire();
                    button(scene, "#skip-cutscene").fire();
                    require(session.simulation().orElseThrow().success(), "Modular ship did not return data");
                    save(scene, output.resolve("09-report.png"));
                    button(scene, "#open-map").fire();
                    require(session.levels().allDone(), "The road map does not show the mission complete");
                    save(scene, output.resolve("09b-road-map-complete.png"));
                    button(scene, "#level-liftoff").fire();
                    require(session.step() == MissionStep.REPORT, "The finished flight did not reopen on its report");
                    button(scene, ".primary").fire();
                    require(session.step() == MissionStep.SHIP && session.build().isComplete()
                            && session.simulation().isEmpty(), "Redesign did not retain the ship and clear the flight");
                    if (height == 800) {
                        stage.close();
                        start(660);
                    } else {
                        verifyMoon(scene, session, () -> verifyMars(scene, session, () -> watchEnding(scene, session, () -> {
                            stage.close();
                            done.complete(null);
                        })));
                    }
                });
                });
            });
        });
    }

    /** After the last mission the report offers the ending cutscene. */
    private void watchEnding(Scene scene, GameSession session, CheckedAction next) {
        require(session.finishedFinalMission(), "Flying Mars did not finish the final mission");
        var ending = button(scene, "#watch-ending");
        require(ending.isVisible(), "The final report does not offer the ending");
        ending.fire();
        after(2800, () -> {
            save(scene, folder.resolve("1280x660/16-ending.png"));
            button(scene, "#skip-cutscene").fire();
            require(lookup(scene, ".game-title") != null, "The ending did not return to the title screen");
            next.run();
        });
    }

    /** From the road map after a level, flies past any mini-game straight into the next mission level. */
    private static void toMissionLevel(Scene scene) {
        for (int i = 0; i < 4; i++) {
            if (lookup(scene, "#skip-game") != null) {
                button(scene, "#skip-game").fire();
            } else if (lookup(scene, "#play-level") != null) {
                button(scene, "#play-level").fire();
            } else {
                return;
            }
        }
    }

    private void verifyMoon(Scene scene, GameSession session, CheckedAction next) throws IOException {
        startMission(scene, "moon-landing", "saturn-v", "apollo-service-module", "rl10c-1", "shuttle-fuel-cell",
                "apollo-11-adapter", "apollo-11-lander");
        require(session.difficulty() == com.liftoff.engine.sim.Difficulty.ENGINEER, "Moon must use medium difficulty");
        supplyTab(scene, 0);
        button(scene, "#instrument-lola").fire();
        supplyTab(scene, 1);
        button(scene, "#station-wallops").fire();
        supplyTab(scene, 2);
        var crew = (TextField) lookup(scene, "#supplies");
        crew.setText("invalid");
        require(button(scene, "#review").isDisabled(), "Invalid crew supplies can reach Review");
        crew.setText("20");
        button(scene, "#review").fire();
        require(button(scene, "#confirm").isDisabled(), "Insufficient supplies can confirm the build");
        button(scene, ".action-bar .secondary").fire();
        ((TextField) lookup(scene, "#supplies")).setText("80");
        finishMission(scene, session, next);
    }

    private void verifyMars(Scene scene, GameSession session, CheckedAction next) throws IOException {
        startMission(scene, "mars-drones", "atlas-v-541", "curiosity-cruise", "mr-106", "mmrtg", "mars-2020-entry",
                "perseverance");
        require(session.difficulty() == com.liftoff.engine.sim.Difficulty.MISSION_DIRECTOR,
                "Mars must use hard difficulty");
        supplyTab(scene, 0);
        button(scene, "#instrument-mastcam-z").fire();
        button(scene, "#instrument-supercam").fire();
        supplyTab(scene, 1);
        button(scene, "#station-wallops").fire();
        finishMission(scene, session, next);
    }

    private void startMission(Scene scene, String id, String rocketId, String... partIds) throws IOException {
        button(scene, ".command-bar .button").fire();
        button(scene, "#mission-" + id).fire();
        button(scene, "#skip-cutscene").fire();
        save(scene, current.resolve(id + "-02-road-map.png"));
        button(scene, "#play-level").fire();
        save(scene, current.resolve(id + "-03-briefing.png"));
        button(scene, ".primary").fire();
        require(!lookup(scene, "#orbit").getParent().isVisible(), id + " asks for an Earth orbit");
        chooseRocket(scene, rocketId);
        save(scene, current.resolve(id + "-03b-flight-plan.png"));
        button(scene, "#toShip").fire();
        var slots = com.liftoff.model.ModuleSlot.values();
        for (int index = 0; index < slots.length; index++) {
            part(scene, slots[index].name(), partIds[index]);
        }
        save(scene, current.resolve(id + "-04-ship-builder.png"));
        button(scene, "#supplies").fire();
        toMissionLevel(scene);
    }

    private static void chooseRocket(Scene scene, String id) {
        var rocket = (ChoiceBox<?>) lookup(scene, "#rocket");
        rocket.getSelectionModel().select(rocketIndex(rocket, id));
    }

    private static void supplyTab(Scene scene, int index) {
        ((javafx.scene.control.TabPane) lookup(scene, "#supplyTabs")).getSelectionModel().select(index);
    }

    private void finishMission(Scene scene, GameSession session, CheckedAction next) throws IOException {
        String id = session.design().objective().id();
        save(scene, current.resolve(id + "-05-supplies.png"));
        button(scene, "#review").fire();
        save(scene, current.resolve(id + "-06-review.png"));
        require(!button(scene, "#confirm").isDisabled(), "Build issues: " + session.buildReport());
        button(scene, "#confirm").fire();
        toMissionLevel(scene);
        button(scene, "#check").fire();
        require(session.launchIssues().isEmpty(), "Launch failed: " + session.launchIssues());
        after(3200, () -> {
            boolean moon = session.design().objective().id().equals("moon-landing");
            waitForSurface(scene, 50, () -> {
                save(scene, folder.resolve("1280x660/" + (moon ? "12-moon-surface.png" : "13-mars-surface.png")));
                button(scene, "#skip-playback").fire();
                button(scene, "#report").fire();
                    button(scene, "#skip-cutscene").fire();
                require(session.step() == MissionStep.REPORT && session.simulation().orElseThrow().success(),
                        "Mission did not fly through to its report");
                save(scene, current.resolve(id + "-09-report.png"));
                ((javafx.scene.control.ScrollPane) lookup(scene, ".clear-scroll")).setVvalue(1);
                save(scene, current.resolve(id + "-09c-report-better.png"));
                next.run();
            });
        });
    }

    private void waitForSurface(Scene scene, int attempts, CheckedAction action) throws Exception {
        var phase = (javafx.scene.control.Label) lookup(scene, "#flight-phase");
        if (phase.getText().equals("Exploring the surface")) {
            action.run();
        } else {
            require(attempts > 0, "Expected surface scene, saw " + phase.getText());
            after(160, () -> waitForSurface(scene, attempts - 1, action));
        }
    }

    private static javafx.scene.control.TabPane verifyCabin(Scene scene, String selector) {
        var views = (javafx.scene.control.TabPane) lookup(scene, selector);
        views.getSelectionModel().select(1);
        button(scene, "#cabin-forward").fire();
        require(((javafx.scene.control.Label) lookup(scene, "#cabin-station")).getText().equals("Pilot seat"),
                "Cabin camera did not advance to the pilot seat");
        return views;
    }

    private void after(int milliseconds, CheckedAction action) {
        var wait = new PauseTransition(Duration.millis(milliseconds));
        wait.setOnFinished(event -> verify(action));
        wait.play();
    }

    private void verify(CheckedAction action) {
        try {
            action.run();
        } catch (Exception | AssertionError problem) {
            done.completeExceptionally(problem);
        }
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }

    private static void part(Scene scene, String slot, String partId) {
        ((ToggleButton) lookup(scene, "#slot-" + slot)).fire();
        button(scene, "#part-" + partId).fire();
    }

    private static javafx.scene.control.ButtonBase button(Scene scene, String selector) {
        var button = (javafx.scene.control.ButtonBase) lookup(scene, selector);
        require(button != null, "Missing button " + selector);
        return button;
    }

    private static javafx.scene.Node lookup(Scene scene, String selector) {
        scene.getRoot().applyCss();
        scene.getRoot().layout();
        return scene.lookup(selector);
    }

    private static int orbitIndex(ChoiceBox<?> choice, String id) {
        return choice.getItems().indexOf(choice.getItems().stream().map(Orbit.class::cast)
                .filter(orbit -> orbit.id().equals(id)).findFirst().orElseThrow());
    }

    private static int rocketIndex(ChoiceBox<?> choice, String id) {
        return choice.getItems().indexOf(choice.getItems().stream().map(LaunchVehicle.class::cast)
                .filter(rocket -> rocket.id().equals(id)).findFirst().orElseThrow());
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void save(Scene scene, Path file) throws IOException {
        WritableImage image = scene.snapshot(null);
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        int[] pixels = new int[width * height];
        image.getPixelReader().getPixels(0, 0, width, height,
                javafx.scene.image.PixelFormat.getIntArgbInstance(), pixels, 0, width);
        var buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        buffered.setRGB(0, 0, width, height, pixels, 0, width);
        ImageIO.write(buffered, "png", file.toFile());
    }
}
