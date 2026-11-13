package com.liftoff.render3d;

import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;

/** A capsule cabin with three camera positions along the aisle. */
public final class CabinView extends StackPane {
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final Group pilot = new Group();
    private final Sphere[] stars = new Sphere[46];

    public CabinView(boolean inFlight) {
        var room = new Group();
        room.getChildren().addAll(box(8, .2, 11, 0, 2, 1, "#354458"),
                box(8, .2, 11, 0, -3, 1, "#5B677A"),
                box(.2, 5, 11, -4, -.5, 1, "#6F7F99"),
                box(.2, 5, 11, 4, -.5, 1, "#6F7F99"),
                box(8, 5, .2, 0, -.5, 6.5, "#4B5C78"));
        for (int side : new int[]{-1, 1}) {
            var ceilingShoulder = box(2.1, .2, 10.7, side * 3.25, -2.43, 1, "#8C9AB0");
            ceilingShoulder.setRotationAxis(Rotate.Z_AXIS);
            ceilingShoulder.setRotate(side * 38);
            var lowerPanel = box(.11, 1.4, 10.8, side * 3.86, 1.15, 1, "#16305E");
            room.getChildren().addAll(ceilingShoulder, lowerPanel);
        }
        for (int z = -3; z <= 6; z += 3) {
            for (int side : new int[]{-1, 1}) {
                room.getChildren().add(box(.3, 4.8, .24, 3.86 * side, -.5, z, "#33609A"));
                room.getChildren().add(lamp(.13, .09, 1.7, 3.65 * side, -2.45, z, "#FFB457"));
            }
        }

        room.getChildren().add(box(3.8, .7, 1.1, 0, .45, 4.1, "#2A3040"));
        var instrument = lamp(2.4, .5, .1, 0, .1, 3.53, "#154259");
        instrument.setRotationAxis(Rotate.X_AXIS);
        instrument.setRotate(-20);
        room.getChildren().add(instrument);
        for (int i = 0; i < 6; i++) {
            room.getChildren().add(lamp(.20, .06 + i * .032, .045, -.97 + i * .37, .10, 3.448,
                    i < 4 ? "#26E3F2" : "#FFD273"));
        }
        for (int i = -4; i <= 4; i++) {
            room.getChildren().add(lamp(.17, .07, .09, i * .29, .47, 3.49, i % 2 == 0 ? "#FFD273" : "#26E3F2"));
        }

        var rim = new Cylinder(1.63, .18, 64);
        rim.setRotationAxis(Rotate.X_AXIS);
        rim.setRotate(90);
        rim.setTranslateY(-1.36);
        rim.setTranslateZ(6.24);
        rim.setMaterial(ModelLibrary.lit("#22E3F2", "#0B6F7A"));
        room.getChildren().add(rim);
        var window = new Cylinder(1.43, .19, 64);
        window.setRotationAxis(Rotate.X_AXIS);
        window.setRotate(90);
        window.setTranslateY(-1.36);
        window.setTranslateZ(6.13);
        window.setMaterial(ModelLibrary.lit(inFlight ? "#071326" : "#34769C", "#091C34"));
        room.getChildren().add(window);
        var random = new java.util.SplittableRandom(20);
        for (int i = 0; i < stars.length; i++) {
            double angle = random.nextDouble(0, Math.TAU);
            double radius = random.nextDouble(.07, 1.35);
            var star = new Sphere(random.nextDouble(.013, .036), 8);
            star.setTranslateX(Math.cos(angle) * radius);
            star.setTranslateY(-1.36 + Math.sin(angle) * radius);
            star.setTranslateZ(5.98);
            star.setMaterial(ModelLibrary.lit("#C7EFFF", "#4B9DCC"));
            star.setVisible(inFlight);
            stars[i] = star;
        }
        room.getChildren().addAll(stars);
        pilot.getChildren().addAll(box(1.23, .29, 1.12, 0, 1.3, 1.1, "#1B314E"),
                box(1.23, 1.5, .27, 0, .62, .51, "#263E5E"),
                box(.91, .90, .47, 0, .56, 1.05, "#E8E8DF"),
                box(.94, .16, .5, 0, .45, 1.05, "#FF7A1A"),
                box(.27, .26, .9, -.26, 1.09, 1.58, "#C4CDD6"),
                box(.27, .26, .9, .26, 1.09, 1.58, "#C4CDD6"));
        var helmet = new Sphere(.42, 24);
        helmet.setTranslateY(-.28);
        helmet.setTranslateZ(1.13);
        helmet.setMaterial(ModelLibrary.surface("#F4F3E8"));
        var visor = new Sphere(.35, 24);
        visor.setTranslateY(-.28);
        visor.setTranslateZ(1.32);
        visor.setScaleZ(.59);
        visor.setMaterial(ModelLibrary.surface("#255178"));
        pilot.getChildren().addAll(helmet, visor);
        room.getChildren().add(pilot);

        camera.setNearClip(.08);
        camera.setFarClip(45);
        camera.setFieldOfView(55);
        var light = new PointLight(Color.web("#D5E9FF"));
        light.setTranslateY(-2);
        light.setTranslateZ(.8);
        var world = new Group(room, camera, light, new AmbientLight(Color.web("#8590AC")));
        var scene = new SubScene(world, 1, 1, true, SceneAntialiasing.BALANCED);
        scene.setMouseTransparent(true);
        scene.setCamera(camera);
        scene.setFill(Color.web("#071330"));
        scene.widthProperty().bind(widthProperty());
        scene.heightProperty().bind(heightProperty());
        getChildren().add(scene);
        station(0);
    }

    public void station(int station) {
        camera.setTranslateX(station == 1 ? 1.43 : 0);
        camera.setTranslateY(station == 2 ? -.20 : -.17);
        camera.setTranslateZ(switch (station) { case 1 -> -.95; case 2 -> 2.4; default -> -4; });
        camera.setRotationAxis(Rotate.Y_AXIS);
        camera.setRotate(station == 1 ? -17 : 0);
        pilot.setVisible(station != 2);
    }

    public void update(double progress) {
        for (int i = 0; i < stars.length; i++) {
            stars[i].setOpacity(.45 + .5 * Math.pow(Math.sin(progress * 38 + i * 1.6), 2));
        }
    }

    private static Box box(double w, double h, double d, double x, double y, double z, String color) {
        var box = new Box(w, h, d);
        box.setTranslateX(x);
        box.setTranslateY(y);
        box.setTranslateZ(z);
        box.setMaterial(ModelLibrary.surface(color));
        return box;
    }

    private static Box lamp(double w, double h, double d, double x, double y, double z, String color) {
        var lamp = box(w, h, d, x, y, z, color);
        lamp.setMaterial(ModelLibrary.lit(color, color));
        return lamp;
    }
}
