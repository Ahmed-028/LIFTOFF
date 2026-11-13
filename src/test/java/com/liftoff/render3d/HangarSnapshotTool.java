package com.liftoff.render3d;

import com.liftoff.model.ModuleSlot;
import javafx.animation.PauseTransition;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.image.PixelFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.FutureTask;

/** Captures the demo's closed and open states with the same scene snapshot method as SnapshotTool. */
public final class HangarSnapshotTool {
    private HangarSnapshotTool() {
    }

    public static void main(String[] args) throws Exception {
        Path folder = Path.of(args.length == 0 ? "target/snapshots" : args[0]);
        Files.createDirectories(folder);
        var capture = new FutureTask<Void>(() -> {
            if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
                throw new IllegalStateException("JavaFX hardware-accelerated 3D is unavailable");
            }
            verifyPicking();
            Scene scene = HangarDemo.createScene();
            save(scene, folder.resolve("hangar-closed.png"));
            Event.fireEvent(scene, key(KeyCode.DIGIT3));
            Event.fireEvent(scene, key(KeyCode.DIGIT5));
            verifyPayloadSnap(scene);
            var waitForDoors = new PauseTransition(Duration.millis(320));
            waitForDoors.setOnFinished(event -> {
                try {
                    save(scene, folder.resolve("hangar-open.png"));
                } catch (IOException ex) {
                    ex.printStackTrace();
                    System.exit(1);
                } finally {
                    Platform.exit();
                }
            });
            waitForDoors.play();
            return null;
        });
        Platform.startup(capture);
        try {
            capture.get();
        } catch (Exception ex) {
            Platform.exit();
            throw ex;
        }
    }

    private static void verifyPayloadSnap(Scene scene) {
        var pane = (Pane) ((BorderPane) scene.getRoot()).getCenter();
        var viewport = (SubScene) pane.getChildren().getFirst();
        var world = (Group) viewport.getRoot();
        var assembly = (Group) world.getChildren().get(1);
        var payloadMount = (Group) assembly.getChildren().get(ModuleSlot.SATELLITE.ordinal());
        var content = (Group) payloadMount.getChildren().getFirst();
        if (!payloadMount.isVisible() || content.getChildren().getFirst().getScaleX() >= 1) {
            throw new IllegalStateException("The payload shortcut must open the bay and start the snap animation");
        }
    }

    private static void verifyPicking() {
        var camera = new OrbitCamera();
        var ship = new ShipAssembly(new ModelLibrary());
        ship.showPart(ModuleSlot.AIRFRAME, "shuttle-orbiter");
        ship.showPart(ModuleSlot.CARGO_BAY, "bay-3u");
        ship.showPart(ModuleSlot.SATELLITE, "cubesat-3u");
        var viewport = new SubScene(new Group(camera.rig(), ship.node()), 640, 480, true, SceneAntialiasing.DISABLED);
        camera.install(viewport);
        viewport.setLayoutX(70);
        viewport.setLayoutY(50);
        new Scene(new Pane(viewport), 800, 620);
        var picker = new SlotPicker(viewport, camera, ship);

        Point3D payload = ship.anchor(ModuleSlot.SATELLITE).localToScene(new Point3D(.12, .11, 0));
        Point2D screen = project(viewport, payload);
        var closed = picker.slotAt(screen.getX(), screen.getY());
        if (closed != ModuleSlot.CARGO_BAY) {
            throw new IllegalStateException("Closed doors must be picked over the hidden payload, got " + closed);
        }
        ship.setCargoBayOpen(true, false);
        var open = picker.slotAt(screen.getX(), screen.getY());
        if (open != ModuleSlot.SATELLITE) {
            throw new IllegalStateException("Open bay must expose the payload for picking, got " + open);
        }
        ship.setDropHighlight(ModuleSlot.SATELLITE);
        if (picker.slotAt(screen.getX(), screen.getY()) != ModuleSlot.SATELLITE || picker.slotAt(10, 10) != null) {
            throw new IllegalStateException("Slot picking must ignore highlights and points outside the viewport");
        }
        Point3D original = camera.origin();
        camera.orbit(70, -30);
        camera.zoom(120);
        Point3D moved = camera.origin();
        if (original.equals(moved)) {
            throw new IllegalStateException("Orbit and zoom must move the camera");
        }
        ship.showPart(ModuleSlot.AIRFRAME, "orion");
        if (!moved.equals(camera.origin())) {
            throw new IllegalStateException("Swapping the airframe must preserve the camera");
        }
        ship.animate(ModuleSlot.AIRFRAME);
        ship.setCargoBayOpen(false, true);
        ship.setCalmMode(true);
        if (ship.partNode(ModuleSlot.AIRFRAME).getScaleX() != 1 || ship.anchor(ModuleSlot.SATELLITE).isVisible()) {
            throw new IllegalStateException("Calm mode must settle a snap and finish closing the bay");
        }
    }

    private static Point2D project(SubScene viewport, Point3D point) {
        var camera = (PerspectiveCamera) viewport.getCamera();
        Point3D relative = camera.sceneToLocal(point);
        double focalLength = viewport.getHeight() / (2 * Math.tan(Math.toRadians(camera.getFieldOfView() / 2)));
        return viewport.localToScene(viewport.getWidth() / 2 + relative.getX() * focalLength / relative.getZ(),
                viewport.getHeight() / 2 + relative.getY() * focalLength / relative.getZ(), true);
    }

    private static KeyEvent key(KeyCode code) {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false);
    }

    private static void save(Scene scene, Path file) throws IOException {
        var image = scene.snapshot(null);
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        var buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = ((DataBufferInt) buffered.getRaster().getDataBuffer()).getData();
        image.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
        ImageIO.write(buffered, "png", file.toFile());
    }
}
