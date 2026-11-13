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
    private final Sphere heat;
    private final Sphere shadow;
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
        ship.getTransforms().addAll(shipTurn, shipPitch);
        var model = parts.get(ModuleSlot.SATELLITE);
        payload = framed(library.create(model == null ? "cubesat-3u" : model), 4.6);
        payload.getTransforms().add(payloadTurn);

        earth = globe("earth", "#427DAD");
        target = globe(destination.name().toLowerCase(Locale.ROOT), destination == Destination.MARS ? "#BF683E" : "#9099A7");
        target.getTransforms().add(targetTurn);
        heat = new Sphere(2.5, 28);
        heat.setMaterial(ModelLibrary.lit("#EB662F", "#75320F"));
        heat.setOpacity(.35);
        heat.setScaleZ(.48);
        shadow = new Sphere(1.2, 24);
        shadow.setMaterial(ModelLibrary.surface("#201C23"));
        shadow.setScaleY(.045);
        shadow.setOpacity(.4);

        var camera = new PerspectiveCamera(true);
        camera.setNearClip(.1);
        camera.setFarClip(150);
        camera.setFieldOfView(40);
        camera.setTranslateZ(-19);
        var light = new PointLight(Color.web("#FFF1D9"));
        light.setTranslateX(-10);
        light.setTranslateY(-9);
        light.setTranslateZ(-12);
        var world = new Group(earth, target, heat, ship, shadow, payload, camera, light,
                new AmbientLight(Color.web("#68758F")));
        var scene = new SubScene(world, 1, 1, true, SceneAntialiasing.BALANCED);
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
        terrain.setVisible(surface);
        earth.setVisible(phase == Phase.DEPARTURE || phase == Phase.RETURN);
        target.setVisible(!surface && phase != Phase.DEPARTURE && phase != Phase.RETURN);
        ship.setVisible(!surface);
        payload.setVisible(surface || orbit);
        heat.setVisible(phase == Phase.ENTRY);

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

        ship.setTranslateX(orbit ? -2.6 + p * 2.2 : -3.9 + p * 4.1);
        ship.setTranslateY(-1.35 + Math.sin(p * 9) * .6);
        shipTurn.setAngle(-56 + 120 * p);
        shipPitch.setAngle(phase == Phase.ENTRY ? 24 : -16 + p * 12);
        heat.setTranslateX(ship.getTranslateX());
        heat.setTranslateY(ship.getTranslateY() + .7);
        heat.setTranslateZ(ship.getTranslateZ() + 1.5);

        double touchdown = Math.clamp((p - .57) / .12, 0, 1);
        payload.setTranslateX(orbit ? .7 + p * 3 : (p - .73) * (destination == Destination.MARS ? 6 : 2));
        payload.setTranslateY(orbit ? -2.25 : -1.5 + touchdown * 3.6);
        payload.setTranslateZ(orbit ? 1 : 1.8 - touchdown * 4.3);
        double payloadScale = orbit ? .25 : 1;
        payload.setScaleX(payloadScale);
        payload.setScaleY(payloadScale);
        payload.setScaleZ(payloadScale);
        payloadTurn.setAngle(orbit ? 35 + p * 210 : -54 + touchdown * 20 + (p - .69) * 52);
        shadow.setVisible(surface);
        shadow.setTranslateX(payload.getTranslateX());
        shadow.setTranslateY(3.12);
        shadow.setTranslateZ(payload.getTranslateZ() + .3);
        shadow.setOpacity(.12 + touchdown * .33);
    }

    private void drawStars() {
        var graphics = stars.getGraphicsContext2D();
        double width = stars.getWidth();
        double height = stars.getHeight();
        graphics.setFill(Color.web("#050C20"));
        graphics.fillRect(0, 0, width, height);
        var random = new SplittableRandom(40);
        for (int i = 0; i < 250; i++) {
            double x = random.nextDouble() * width;
            double y = random.nextDouble() * height;
            double size = random.nextDouble(.6, 2.2);
            graphics.setFill(Color.color(.75, .88, 1, random.nextDouble(.12, .85)));
            graphics.fillOval(x, y, size, size);
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
