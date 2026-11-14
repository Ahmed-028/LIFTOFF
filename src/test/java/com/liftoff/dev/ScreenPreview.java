package com.liftoff.dev;

import com.liftoff.app.LiftoffApp;
import com.liftoff.app.SceneNavigator;
import com.liftoff.app.Screen;
import com.liftoff.data.CatalogLoader;
import com.liftoff.session.GameSession;
import com.liftoff.session.Level;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.PixelFormat;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


public final class ScreenPreview {

    public static void main(String[] args) throws Exception {
        Path folder = Path.of(args[0]);
        String screen = args[1];
        String mission = args[2];
        int height = Integer.parseInt(args[3]);
        System.setProperty("liftoff.sound", "off");
        var done = new CompletableFuture<Void>();
        Platform.startup(() -> {
            try {
                Platform.setImplicitExit(false);
                LiftoffApp.loadFonts();
                var session = new GameSession(CatalogLoader.loadFromClasspath());
                var scene = new Scene(new StackPane(), 1280, height);
                scene.getStylesheets().add(LiftoffApp.class.getResource(LiftoffApp.STYLESHEET).toExternalForm());
                var navigator = new SceneNavigator(scene, session, ResourceBundle.getBundle(LiftoffApp.MESSAGES_BUNDLE));
                navigator.setScreenDecorator(new com.liftoff.ui.NovaGuide(navigator, session,
                        ResourceBundle.getBundle(LiftoffApp.MESSAGES_BUNDLE))::decorate);
                var stage = new Stage();
                stage.setScene(scene);
                stage.show();
                session.selectMission(mission);
                switch (screen) {
                    case "ROAD_MAP_TRAVEL" -> {
                        session.next();
                        navigator.finishLevel(Level.BUILD);
                    }
                    case "STAR_MATCH", "ASTEROID_DASH" -> {
                        session.next();
                        if (screen.equals("ASTEROID_DASH")) {
                            // Previews skip earlier levels.
                        }
                        navigator.show(Screen.valueOf(screen));
                    }
                    case "ROAD_MAP" -> navigator.showRoadMap(List.of());
                    case "INTRO" -> navigator.playIntro();
                    case "MISSION_CUTSCENE" -> navigator.playCutscene(com.liftoff.app.Cutscene.MISSION, () -> { });
                    case "ENDING" -> navigator.playCutscene(com.liftoff.app.Cutscene.ENDING, () -> { });
                    case "ARRIVAL_WIN", "ARRIVAL_FAIL" -> {
                        var destination = session.catalog().mission(mission).orElseThrow().destination();
                        var view = new com.liftoff.render3d.ArrivalScene(destination, screen.equals("ARRIVAL_WIN"));
                        scene.setRoot(new StackPane(view));
                        long begin = System.nanoTime();
                        new javafx.animation.AnimationTimer() {
                            @Override
                            public void handle(long now) {
                                view.update((now - begin) / 1e9);
                            }
                        }.start();
                    }
                    default -> navigator.show(Screen.valueOf(screen));
                }
                Files.createDirectories(folder);
                capture(scene, folder, screen.toLowerCase(), args, 4, done);
            } catch (Exception problem) {
                done.completeExceptionally(problem);
            }
        });
        try {
            done.get(60, TimeUnit.SECONDS);
        } finally {
            Platform.exit();
        }
    }

    private static void capture(Scene scene, Path folder, String name, String[] args, int index,
            CompletableFuture<Void> done) {
        if (index >= args.length) {
            done.complete(null);
            return;
        }
        int delay = Integer.parseInt(args[index]);
        var wait = new PauseTransition(Duration.millis(delay));
        wait.setOnFinished(event -> {
            try {
                var image = scene.snapshot(null);
                int width = (int) image.getWidth();
                int height = (int) image.getHeight();
                int[] pixels = new int[width * height];
                image.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
                var buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                buffered.setRGB(0, 0, width, height, pixels, 0, width);
                ImageIO.write(buffered, "png", folder.resolve(name + "-" + index + ".png").toFile());
                // Start rides automatically in animated previews.
                if (scene.lookup("#start-ride") instanceof javafx.scene.control.Button start) {
                    start.fire();
                }
                capture(scene, folder, name, args, index + 1, done);
            } catch (Exception problem) {
                done.completeExceptionally(problem);
            }
        });
        wait.play();
    }
}
