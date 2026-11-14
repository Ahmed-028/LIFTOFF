package com.liftoff.render3d;

import com.liftoff.render.Theme;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Box;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;


public abstract class CutsceneView extends StackPane {

    protected final Group world = new Group();
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final Translate cameraAt = new Translate();
    private final Rotate cameraYaw = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate cameraPitch = new Rotate(0, Rotate.X_AXIS);

    protected CutsceneView(Color background, Color ambient) {
        camera.setNearClip(.05);
        camera.setFarClip(200);
        camera.setFieldOfView(42);
        camera.getTransforms().setAll(cameraAt, cameraYaw, cameraPitch);
        var scene = new SubScene(new Group(world, camera, new AmbientLight(ambient)), 1, 1, true,
                SceneAntialiasing.BALANCED);
        scene.setCamera(camera);
        scene.setFill(background);
        scene.setMouseTransparent(true);
        scene.widthProperty().bind(widthProperty());
        scene.heightProperty().bind(heightProperty());
        getChildren().add(scene);
        setMinSize(0, 0);
        setStyle("-fx-background-color: " + toWeb(Theme.NAVY_950) + ";");
    }


    public abstract double length();


    public abstract void update(double seconds);


    protected void look(double x, double y, double z, double tx, double ty, double tz) {
        cameraAt.setX(x);
        cameraAt.setY(y);
        cameraAt.setZ(z);
        double dx = tx - x;
        double dy = ty - y;
        double dz = tz - z;
        double length = Math.max(1e-6, Math.sqrt(dx * dx + dy * dy + dz * dz));
        cameraYaw.setAngle(Math.toDegrees(Math.atan2(dx, dz)));
        cameraPitch.setAngle(-Math.toDegrees(Math.asin(dy / length)));
    }

    protected static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }


    protected static double ease(double t) {
        return t * t * (3 - 2 * t);
    }

    protected static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    protected static double lerpAngle(double from, double to, double t) {
        double delta = ((to - from) % 360 + 540) % 360 - 180;
        return from + delta * t;
    }


    protected static double phase(double t, double start, double duration) {
        return ease(clamp((t - start) / duration));
    }

    protected static Box box(double w, double h, double d, double x, double y, double z, String colour) {
        var box = new Box(w, h, d);
        box.setMaterial(ModelLibrary.surface(colour));
        return Meshes.at(box, x, y, z);
    }

    protected static Sphere sphere(double radius, String colour) {
        var sphere = new Sphere(radius, 32);
        sphere.setMaterial(ModelLibrary.surface(colour));
        return sphere;
    }

    protected static Sphere glow(double radius, String colour) {
        var sphere = new Sphere(radius, 16);
        sphere.setMaterial(ModelLibrary.lit(colour, colour));
        return sphere;
    }

    protected static <N extends Node> N at(N node, double x, double y, double z) {
        return Meshes.at(node, x, y, z);
    }

    private static String toWeb(Color colour) {
        return String.format("#%02X%02X%02X", (int) Math.round(colour.getRed() * 255),
                (int) Math.round(colour.getGreen() * 255), (int) Math.round(colour.getBlue() * 255));
    }
}
