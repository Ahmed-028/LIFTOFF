package com.liftoff.render3d;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

/** Opposed hinged doors with their pivots on the top outside edges of the bay. */
final class CargoBayDoors {
    private final Group root = new Group();
    private final Rotate left = new Rotate(0, Rotate.Z_AXIS);
    private final Rotate right = new Rotate(0, Rotate.Z_AXIS);
    private final DoubleProperty position = new SimpleDoubleProperty();
    private Timeline animation;
    private boolean open;
    private double halfSpan;

    CargoBayDoors() {
        left.angleProperty().bind(position.multiply(-150));
        right.angleProperty().bind(position.multiply(150));
    }

    Group node() {
        return root;
    }

    void fit(Bounds bay) {
        root.getChildren().clear();
        if (bay == null) {
            halfSpan = 0;
            return;
        }
        halfSpan = bay.getWidth() * .5;
        double center = (bay.getMinZ() + bay.getMaxZ()) * .5;
        root.getChildren().setAll(leaf(-1, center, bay.getDepth(), bay.getMinY(), left),
                leaf(1, center, bay.getDepth(), bay.getMinY(), right));
    }

    private Group leaf(int side, double center, double length, double top, Rotate rotation) {
        PhongMaterial shell = ModelLibrary.surface("#C7D4E6");
        PhongMaterial rib = ModelLibrary.surface("#59718E");
        double width = halfSpan * .98;
        var panel = box(width, .11, length, -side * width / 2, 0, 0, shell);
        var edge = box(.075, .125, length, -side * (width - .04), 0, 0, rib);
        var marking = box(width * .5, .015, .085, -side * width * .5, -.06, length * .31,
                ModelLibrary.surface("#E7833D"));
        var hinge = new Group(panel, edge, marking);
        hinge.getTransforms().add(rotation);
        hinge.setTranslateX(side * halfSpan);
        hinge.setTranslateY(top - .035);
        hinge.setTranslateZ(center);
        return hinge;
    }

    void setOpen(boolean open, boolean animate, Runnable settled) {
        double start = position.get();
        if (animation != null) {
            animation.stop();
            animation = null;
        }
        this.open = open;
        double target = open ? 1 : 0;
        if (!animate || start == target) {
            position.set(target);
            settled.run();
            return;
        }
        animation = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(position, start)),
                new KeyFrame(Duration.millis(260 * Math.abs(target - start)),
                        new KeyValue(position, target, Interpolator.EASE_BOTH)));
        animation.setOnFinished(event -> {
            animation = null;
            settled.run();
        });
        animation.play();
    }

    void settle(Runnable settled) {
        setOpen(open, false, settled);
    }

    double leftAngle() {
        return left.getAngle();
    }

    double halfSpan() {
        return halfSpan;
    }

    private static Box box(double w, double h, double d, double x, double y, double z, PhongMaterial material) {
        var box = new Box(w, h, d);
        box.setTranslateX(x);
        box.setTranslateY(y);
        box.setTranslateZ(z);
        box.setMaterial(material);
        return box;
    }
}
