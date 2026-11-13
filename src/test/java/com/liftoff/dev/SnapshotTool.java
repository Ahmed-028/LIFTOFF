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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

/** Clicks through the shipped UI and captures each screen at both supported QA sizes. */
public final class SnapshotTool {
    /** The navigator of the run in progress; the screens are brought up to date before every look. */
    private static SceneNavigator navigator;
    private final Path folder;
    private final CompletableFuture<Void> done = new CompletableFuture<>();

    private SnapshotTool(Path folder) {
        this.folder = folder;
    }

    public static void main(String[] args) throws Exception {
        Path folder = Path.of(args.length > 0 ? args[0] : "target/snapshots");
        var tool = new SnapshotTool(folder);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            LiftoffApp.loadFonts();
            tool.start(800);
        });
        try {
            tool.done.get(60, TimeUnit.SECONDS);
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
            navigator = new SceneNavigator(scene, session, messages);
            var stage = new Stage();
            stage.setScene(scene);
            stage.show();

            var output = folder.resolve("1280x" + height);
            Files.createDirectories(output);
            navigator.show(Screen.TITLE);
            save(scene, output.resolve("01-title.png"));
            button(scene, ".primary").fire();
            save(scene, output.resolve("02-mission-select.png"));
            navigator.showError(new IllegalStateException("Snapshot of the error screen"));
            save(scene, output.resolve("12-error.png"));
            button(scene, "#error-back").fire();
            require(lookup(scene, "#mission-farm-watch") != null, "Go back did not return to the missions");
            button(scene, "#open-sources").fire();
            save(scene, output.resolve("10-sources.png"));
            var sources = (TitledPane) lookup(scene, ".titled-pane");
            sources.setAnimated(false);
            sources.setExpanded(true);
            require(lookup(scene, ".hyperlink") != null, "Source citations are missing");
            button(scene, ".secondary").fire();

            button(scene, "#mission-farm-watch").fire();
            require(session.step() == MissionStep.BRIEFING, "Mission card did not open the briefing");
            save(scene, output.resolve("03-briefing.png"));
            button(scene, ".primary").fire();
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
                require(session.step() == MissionStep.SUPPLIES, "Supplies step did not open");
                var airframeChoice = (ChoiceBox<?>) lookup(scene, "#airframe");
                int fittedAirframe = airframeChoice.getSelectionModel().getSelectedIndex();
                double originalMass = session.missionResources().get(com.liftoff.engine.ResourceType.MASS).used();
                double originalBudget = session.missionResources().get(com.liftoff.engine.ResourceType.BUDGET).used();
                airframeChoice.getSelectionModel().select((fittedAirframe + 1) % airframeChoice.getItems().size());
                require(originalMass != session.missionResources().get(com.liftoff.engine.ResourceType.MASS).used()
                                && originalBudget != session.missionResources().get(com.liftoff.engine.ResourceType.BUDGET).used(),
                        "Changing the airframe did not update mass and budget");
                airframeChoice.getSelectionModel().select(fittedAirframe);
                var orbit = (ChoiceBox<?>) lookup(scene, "#orbit");
                orbit.getSelectionModel().select(orbitIndex(orbit, "sso-705"));
                supplyTab(scene, 1);
                button(scene, "#instrument-ali").fire();
                save(scene, output.resolve("11-instrument-preview.png"));
                supplyTab(scene, 2);
                button(scene, "#station-svalbard").fire();
                button(scene, "#station-fairbanks").fire();
                supplyTab(scene, 0);
                var rocket = (ChoiceBox<?>) lookup(scene, "#rocket");
                rocket.getSelectionModel().select(rocketIndex(rocket, "electron"));
                button(scene, "#review").fire();
                button(scene, "#confirm").fire();
                button(scene, "#check").fire();
                require(session.launchIssues().stream().anyMatch(issue ->
                                issue.kind() == com.liftoff.engine.sim.LaunchIssue.Kind.MASS_OVER_CAPACITY),
                        "Launch did not show the inadequate rocket warning");
                button(scene, ".action-bar .secondary").fire();
                button(scene, ".action-bar .secondary").fire();
                rocket = (ChoiceBox<?>) lookup(scene, "#rocket");
                rocket.getSelectionModel().select(rocketIndex(rocket, "atlas-v-401"));
                save(scene, output.resolve("05-supplies.png"));
                button(scene, ".primary").fire();
                save(scene, output.resolve("06-review.png"));
                require(!button(scene, "#confirm").isDisabled(), "Valid build has issues: " + session.buildReport());
                button(scene, "#confirm").fire();
                save(scene, output.resolve("07-launch.png"));
                button(scene, "#check").fire();
                require(session.launchIssues().isEmpty(), "Launch failed: " + session.launchIssues());
                after(3200, () -> {
                    require(session.step() == MissionStep.FLY, "Countdown did not open flight");
                    button(scene, "#skip-playback").fire();
                    save(scene, output.resolve("08-fly.png"));
                    button(scene, "#report").fire();
                    require(session.simulation().orElseThrow().success(), "Modular ship did not return data");
                    save(scene, output.resolve("09-report.png"));
                    button(scene, ".primary").fire();
                    require(session.step() == MissionStep.SHIP && session.build().isComplete()
                            && session.simulation().isEmpty(), "Redesign did not retain the ship and clear the flight");
                    if (height == 800) {
                        stage.close();
                        start(660);
                    } else {
                        verifyMoon(scene, session, () -> verifyMars(scene, session, () -> {
                            stage.close();
                            done.complete(null);
                        }));
                    }
                });
            });
        });
    }

    private void verifyMoon(Scene scene, GameSession session, CheckedAction next) {
        startMission(scene, "moon-landing", "apollo-service-module", "rl10c-1", "shuttle-fuel-cell",
                "apollo-11-adapter", "apollo-11-lander");
        require(session.difficulty() == com.liftoff.model.Difficulty.MEDIUM, "Moon must use medium difficulty");
        supplyTab(scene, 1);
        button(scene, "#instrument-lola").fire();
        supplyTab(scene, 2);
        button(scene, "#station-wallops").fire();
        supplyTab(scene, 0);
        chooseRocket(scene, "saturn-v");
        var crew = (TextField) lookup(scene, "#supplies");
        require(!lookup(scene, "#orbit").getParent().isVisible(), "Moon mission asks for an Earth orbit");
        crew.setText("invalid");
        require(button(scene, "#review").isDisabled(), "Invalid crew supplies can reach Review");
        crew.setText("20");
        button(scene, "#review").fire();
        require(button(scene, "#confirm").isDisabled(), "Insufficient supplies can confirm the build");
        button(scene, ".action-bar .secondary").fire();
        ((TextField) lookup(scene, "#supplies")).setText("80");
        finishMission(scene, session, next);
    }

    private void verifyMars(Scene scene, GameSession session, CheckedAction next) {
        startMission(scene, "mars-drones", "curiosity-cruise", "mr-106", "mmrtg", "mars-2020-entry",
                "perseverance");
        require(session.difficulty() == com.liftoff.model.Difficulty.HARD,
                "Mars must use hard difficulty");
        supplyTab(scene, 1);
        button(scene, "#instrument-mastcam-z").fire();
        button(scene, "#instrument-supercam").fire();
        supplyTab(scene, 2);
        button(scene, "#station-wallops").fire();
        supplyTab(scene, 0);
        chooseRocket(scene, "atlas-v-541");
        finishMission(scene, session, next);
    }

    private static void startMission(Scene scene, String id, String... partIds) {
        button(scene, ".command-bar .button").fire();
        button(scene, "#mission-" + id).fire();
        button(scene, ".primary").fire();
        var slots = com.liftoff.model.ModuleSlot.values();
        for (int index = 0; index < slots.length; index++) {
            part(scene, slots[index].name(), partIds[index]);
        }
        button(scene, "#supplies").fire();
    }

    private static void chooseRocket(Scene scene, String id) {
        var rocket = (ChoiceBox<?>) lookup(scene, "#rocket");
        rocket.getSelectionModel().select(rocketIndex(rocket, id));
    }

    private static void supplyTab(Scene scene, int index) {
        ((javafx.scene.control.TabPane) lookup(scene, "#supplyTabs")).getSelectionModel().select(index);
    }

    private void finishMission(Scene scene, GameSession session, CheckedAction next) {
        button(scene, "#review").fire();
        require(!button(scene, "#confirm").isDisabled(), "Build issues: " + session.buildReport());
        button(scene, "#confirm").fire();
        button(scene, "#check").fire();
        require(session.launchIssues().isEmpty(), "Launch failed: " + session.launchIssues());
        after(3200, () -> {
            button(scene, "#skip-playback").fire();
            button(scene, "#report").fire();
            require(session.step() == MissionStep.REPORT && session.simulation().orElseThrow().success(),
                    "Mission did not fly through to its report");
            next.run();
        });
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
        navigator.settle();
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
        navigator.settle();
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
