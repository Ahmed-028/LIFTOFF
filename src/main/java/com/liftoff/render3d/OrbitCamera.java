package com.liftoff.render3d;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.transform.Rotate;

final class OrbitCamera {
    private static final double MIN_DISTANCE = 13;
    private static final double MAX_DISTANCE = 38;
    private static final double MIN_PITCH = -63;
    private static final double MAX_PITCH = -9;

    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final Rotate pitch = new Rotate(-27, Rotate.X_AXIS);
    private final Rotate yaw = new Rotate(-57, Rotate.Y_AXIS);
    private final Group rig = new Group(new Group(camera));
    private double dragX;
    private double dragY;

    OrbitCamera() {
        camera.setNearClip(.1);
        camera.setFarClip(100);
        camera.setFieldOfView(34);
        camera.setTranslateZ(-23);
        rig.setTranslateY(-.63);
        rig.getTransforms().addAll(yaw, pitch);
    }

    Group rig() {
        return rig;
    }

    void install(SubScene scene) {
        scene.setCamera(camera);
        scene.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                dragX = event.getSceneX();
                dragY = event.getSceneY();
            }
        });
        scene.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            if (event.isPrimaryButtonDown()) {
                orbit(event.getSceneX() - dragX, event.getSceneY() - dragY);
                dragX = event.getSceneX();
                dragY = event.getSceneY();
                event.consume();
            }
        });
        scene.setOnScroll(event -> {
            zoom(event.getDeltaY());
            event.consume();
        });
    }

    void orbit(double dx, double dy) {
        yaw.setAngle(Math.IEEEremainder(yaw.getAngle() - dx * .32, 360));
        pitch.setAngle(Math.clamp(pitch.getAngle() + dy * .28, MIN_PITCH, MAX_PITCH));
    }

    void zoom(double scrollDelta) {
        camera.setTranslateZ(-Math.clamp(-camera.getTranslateZ() * Math.exp(-scrollDelta * .0018),
                MIN_DISTANCE, MAX_DISTANCE));
    }

    /** Yaw and pitch in degrees, then the distance from the ship. */
    double[] pose() {
        return new double[] {yaw.getAngle(), pitch.getAngle(), -camera.getTranslateZ()};
    }

    Point3D origin() {
        return camera.localToScene(Point3D.ZERO);
    }

    Point3D rayThrough(double x, double y, double width, double height) {
        double focalLength = height / (2 * Math.tan(Math.toRadians(camera.getFieldOfView() / 2)));
        return camera.localToScene(new Point3D(x - width / 2, y - height / 2, focalLength));
    }
}
