package com.liftoff.render3d;

import com.liftoff.model.ModuleSlot;
import com.liftoff.session.stub.Mission.Destination;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;

import java.util.Locale;
import java.util.Map;
import java.util.SplittableRandom;

/** Renders an illustrative flight from playback progress. Simulation rules live in the engine. */
public final class FlightView extends StackPane {
    public enum Phase { DEPARTURE, CRUISE, ORBIT, ENTRY, LANDING, SURFACE, RETURN }

    private final Destination destination;
    private final ImageView terrain = new ImageView();
    private final Canvas stars = new Canvas();
    private final Group ship;
    private final Group payload;
    private final Sphere earth;
    private final Sphere target;
    private final Sphere shadow;
    private final Group exhaust = new Group();
    private final Rotate bank = new Rotate(0, Rotate.Z_AXIS);
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final double[] starX = new double[250];
    private final double[] starY = new double[250];
    private final double[] starSize = new double[250];
    private final Color[] starColor = new Color[250];
    private double starProgress;
    private boolean streaks;
    private final Rotate shipTurn = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate shipPitch = new Rotate(0, Rotate.X_AXIS);
    private final Rotate payloadTurn = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate targetTurn = new Rotate(0, Rotate.Y_AXIS);

    public FlightView(Destination destination, Map<ModuleSlot, String> parts) {
        this.destination = destination;
        setMinSize(0, 220);
        getStyleClass().add("flight-viewport");
        var clip = new javafx.scene.shape.Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);
        setAlignment(Pos.CENTER);
        var random = new SplittableRandom(40);
        for (int i = 0; i < starX.length; i++) {
            starX[i] = random.nextDouble();
            starY[i] = random.nextDouble();
            starSize[i] = random.nextDouble(.6, 2.2);
            starColor[i] = Color.color(.75, .88, 1, random.nextDouble(.12, .85));
        }
        stars.widthProperty().bind(widthProperty());
        stars.heightProperty().bind(heightProperty());
        stars.widthProperty().addListener((property, before, value) -> drawStars());
        stars.heightProperty().addListener((property, before, value) -> drawStars());
        if (destination != Destination.EARTH) {
            terrain.setImage(image(destination.name().toLowerCase(Locale.ROOT) + "-terrain.jpg"));
        }
        terrain.fitWidthProperty().bind(widthProperty());
        terrain.fitHeightProperty().bind(heightProperty());
        terrain.setManaged(false);

        var library = new ModelLibrary();
        var assembly = new ShipAssembly(library);
        parts.forEach(assembly::showPart);
        ship = framed(assembly.node(), 4.5);
        ship.getTransforms().addAll(bank, shipTurn, shipPitch);
        for (int i = 0; i < 8; i++) {
            var glow = new Sphere(.42 - i * .043, 16);
            glow.setTranslateZ(2.15 + i * .24);
            glow.setScaleZ(1.65);
            glow.setOpacity(.95 - i * .11);
            glow.setMaterial(ModelLibrary.lit(i < 2 ? "#B8FFFF" : "#3198FF", "#1A54A8"));
            exhaust.getChildren().add(glow);
        }
        ship.getChildren().add(exhaust);
        var model = parts.get(ModuleSlot.SATELLITE);
        payload = framed(library.create(model == null ? "cubesat-3u" : model), 4.6);
        payload.getTransforms().add(payloadTurn);

        earth = globe("earth", "#427DAD");
        target = globe(destination.name().toLowerCase(Locale.ROOT), destination == Destination.MARS ? "#BF683E" : "#9099A7");
        target.getTransforms().add(targetTurn);
        shadow = new Sphere(1.2, 24);
        shadow.setMaterial(ModelLibrary.surface("#201C23"));
        shadow.setScaleY(.045);
        shadow.setOpacity(.4);

        camera.setNearClip(.1);
        camera.setFarClip(150);
        camera.setFieldOfView(40);
        camera.setTranslateZ(-19);
        var light = new PointLight(Color.web("#FFF1D9"));
        light.setTranslateX(-10);
        light.setTranslateY(-9);
        light.setTranslateZ(-12);
        var world = new Group(earth, target, ship, shadow, payload, camera, light,
                new AmbientLight(Color.web("#68758F")));
        var scene = new SubScene(world, 1, 1, true, SceneAntialiasing.BALANCED);
        scene.setMouseTransparent(true);
        scene.setFill(Color.TRANSPARENT);
        scene.setCamera(camera);
        scene.widthProperty().bind(widthProperty());
        scene.heightProperty().bind(heightProperty());
        getChildren().addAll(stars, terrain, scene);
        showProgress(0);
    }

    public static Phase phase(Destination destination, double progress) {
        if (destination == Destination.EARTH) {
            return progress < .18 ? Phase.DEPARTURE : Phase.ORBIT;
        }
        if (progress < .14) {
            return Phase.DEPARTURE;
        }
        if (progress < .42) {
            return Phase.CRUISE;
        }
        if (progress < .57) {
            return destination == Destination.MARS ? Phase.ENTRY : Phase.ORBIT;
        }
        if (progress < .69) {
            return Phase.LANDING;
        }
        if (destination == Destination.MOON && progress >= .90) {
            return Phase.RETURN;
        }
        return Phase.SURFACE;
    }

    public void showProgress(double value) {
        double p = Math.clamp(value, 0, 1);
        Phase phase = phase(destination, p);
        boolean surface = phase == Phase.LANDING || phase == Phase.SURFACE;
        boolean orbit = destination == Destination.EARTH && phase == Phase.ORBIT;
        boolean thrust = phase == Phase.DEPARTURE || phase == Phase.CRUISE || phase == Phase.RETURN;
        double burn = switch (phase) {
            case DEPARTURE -> ease(p / (destination == Destination.EARTH ? .18 : .14));
            case CRUISE -> ease((p - .14) / .28);
            case RETURN -> ease((p - .90) / .10);
            default -> 0;
        };
        exhaust.setVisible(thrust);
        exhaust.setScaleZ(1 + burn * .65 + Math.sin(p * 110) * .12);
        if (!surface) {
            streaks = thrust;
            starProgress = p;
            drawStars();
        }
        terrain.setVisible(surface);
        earth.setVisible(phase == Phase.DEPARTURE || phase == Phase.RETURN);
        target.setVisible(!surface && phase != Phase.DEPARTURE && phase != Phase.RETURN);
        ship.setVisible(!surface);
        payload.setVisible(surface || orbit);

        earth.setRadius(phase == Phase.RETURN ? 5 + (p - .90) * 46 : 10 - p * 31);
        earth.setTranslateX(phase == Phase.RETURN ? 6 : -8.3 - p * 8);
        earth.setTranslateY(2.2);
        earth.setTranslateZ(7);
        earth.setRotate(p * 80);
        earth.setRotationAxis(Rotate.Y_AXIS);

        target.setRadius(orbit ? 10.3 : 2 + 11 * p);
        target.setTranslateX(orbit ? 6 : 6.4 - 3 * p);
        target.setTranslateY(orbit ? 4.9 : 1.1);
        target.setTranslateZ(orbit ? 10 : 10 - p * 4);
        targetTurn.setAngle(35 + 105 * p);

        // Start close behind the ship and let it pull away as it accelerates.
        ship.setTranslateX(thrust ? -2.7 + burn * 3.5 : -2.6 + p * 2.2);
        ship.setTranslateY(thrust ? .7 - burn * 2.4 : -1.35);
        ship.setTranslateZ(thrust ? -3 + burn * 10 : 0);
        bank.setAngle(thrust ? -12 + burn * 23 : 0);
        camera.setTranslateZ(thrust ? -15.6 - burn * 2.6 : -19);
        shipTurn.setAngle(thrust ? 142 - burn * 27 : -52 + 105 * p);
        shipPitch.setAngle(thrust ? -10 - burn * 6 : phase == Phase.ENTRY ? 24 : -9);

        double touchdown = ease((p - .57) / .12);
        double exploration = Math.max(0, p - .69);
        payload.setTranslateX(orbit ? .7 + p * 3 : -.35 + exploration * (destination == Destination.MARS ? 6 : 2));
        payload.setTranslateY(orbit ? -2.25 : -3.7 + touchdown * 5.8);
        payload.setTranslateZ(orbit ? 1 : -2.5);
        double payloadScale = orbit ? .25 : 1;
        payload.setScaleX(payloadScale);
        payload.setScaleY(payloadScale);
        payload.setScaleZ(payloadScale);
        payloadTurn.setAngle(orbit ? 35 + p * 210 : -34 + exploration * 52);
        shadow.setVisible(surface);
        shadow.setTranslateX(payload.getTranslateX());
        shadow.setTranslateY(3.12);
        shadow.setTranslateZ(payload.getTranslateZ() + .3);
        shadow.setOpacity(.12 + touchdown * .33);
    }

    private static double ease(double fraction) {
        double t = Math.clamp(fraction, 0, 1);
        return t * t * (3 - 2 * t);
    }

    private void drawStars() {
        var graphics = stars.getGraphicsContext2D();
        double width = stars.getWidth();
        double height = stars.getHeight();
        graphics.setFill(Color.web("#050C20"));
        graphics.fillRect(0, 0, width, height);
        for (int i = 0; i < starX.length; i++) {
            double offset = streaks ? starProgress * (4.6 + 2 * starSize[i]) : 0;
            double x = ((starX[i] + offset) % 1) * width;
            double y = starY[i] * height;
            graphics.setFill(starColor[i]);
            if (streaks) {
                graphics.setStroke(starColor[i]);
                graphics.setLineWidth(starSize[i] * .6);
                double length = starSize[i] * 14;
                graphics.strokeLine(x, y, x - length, y + length * .35);
            } else {
                graphics.fillOval(x, y, starSize[i], starSize[i]);
            }
        }
    }

    private static Group framed(Group model, double size) {
        var bounds = model.getBoundsInLocal();
        model.setTranslateX(-bounds.getCenterX());
        model.setTranslateY(-bounds.getCenterY());
        model.setTranslateZ(-bounds.getCenterZ());
        double scale = size / Math.max(bounds.getWidth(), Math.max(bounds.getHeight(), bounds.getDepth()));
        var framed = new Group(model);
        framed.getTransforms().add(new Scale(scale, scale, scale));
        return new Group(framed);
    }

    private static Sphere globe(String name, String color) {
        var sphere = new Sphere(1, 64);
        var material = ModelLibrary.surface(color);
        var map = image(name + "-map.jpg");
        if (map != null) {
            material.setDiffuseColor(Color.WHITE);
            material.setDiffuseMap(map);
        }
        material.setSpecularColor(Color.TRANSPARENT);
        sphere.setMaterial(material);
        return sphere;
    }

    private static Image image(String name) {
        var resource = FlightView.class.getResource("/com/liftoff/art/flight/" + name);
        return resource == null ? null : new Image(resource.toExternalForm());
    }
}
