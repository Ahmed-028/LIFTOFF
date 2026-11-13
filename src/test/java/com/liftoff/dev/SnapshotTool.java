package com.liftoff.dev;

import com.liftoff.app.LiftoffApp;
import com.liftoff.app.SceneNavigator;
import com.liftoff.app.Screen;
import com.liftoff.data.CatalogLoader;
import com.liftoff.session.GameSession;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Developer tool, not part of the game: renders every screen to a PNG so the look can be
 * checked without clicking through the app. Usage: SnapshotTool &lt;output folder&gt;
 */
public final class SnapshotTool {

    private SnapshotTool() {
    }

    public static void main(String[] args) throws Exception {
        Path folder = Path.of(args.length > 0 ? args[0] : "target/snapshots");
        Files.createDirectories(folder);

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.startup(() -> {
            try {
                renderAll(folder);
            } catch (Throwable problem) {
                failure.set(problem);
            } finally {
                done.countDown();
            }
        });
        done.await();
        Platform.exit();

        if (failure.get() != null) {
            failure.get().printStackTrace();
            System.exit(1);
        }
        System.out.println("Snapshots written to " + folder.toAbsolutePath());
    }

    private static void renderAll(Path folder) throws IOException {
        ResourceBundle messages = ResourceBundle.getBundle(LiftoffApp.MESSAGES_BUNDLE);
        GameSession session = new GameSession(CatalogLoader.loadFromClasspath());
        Scene scene = new Scene(new StackPane(), 1280, 800);
        scene.getStylesheets().add(LiftoffApp.class.getResource(LiftoffApp.STYLESHEET).toExternalForm());
        SceneNavigator navigator = new SceneNavigator(scene, session, messages);

        navigator.show(Screen.TITLE);
        save(scene, folder.resolve("1-title.png"));

        navigator.show(Screen.MISSION_SELECT);
        save(scene, folder.resolve("2-mission-select.png"));

        session.selectObjective("ice-patrol");
        save(scene, folder.resolve("3-mission-chosen.png"));

        // The same screen on a small laptop display, to check nothing is cut off.
        Scene small = new Scene(new StackPane(), 1280, 660);
        small.getStylesheets().addAll(scene.getStylesheets());
        new SceneNavigator(small, session, messages).show(Screen.MISSION_SELECT);
        save(small, folder.resolve("4-mission-chosen-small-window.png"));
    }

    private static void save(Scene scene, Path file) throws IOException {
        WritableImage image = scene.snapshot(null);
        PixelReader pixels = image.getPixelReader();
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                buffered.setRGB(x, y, pixels.getArgb(x, y));
            }
        }
        ImageIO.write(buffered, "png", file.toFile());
    }
}
