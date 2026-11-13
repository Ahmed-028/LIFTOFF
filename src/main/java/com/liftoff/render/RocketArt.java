package com.liftoff.render;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineJoin;

/** The LIFTOFF rocket, drawn from shapes on a 200 x 310 grid and scaled to the size asked for. */
public final class RocketArt {

    private static final double DESIGN_HEIGHT = 310;

    private RocketArt() {
    }

    public static Node create(double height) {
        Polygon flame = new Polygon(72, 240, 100, 305, 128, 240);
        flame.setFill(Palette.POWER);

        Polygon innerFlame = new Polygon(86, 240, 100, 278, 114, 240);
        innerFlame.setFill(Palette.ROCKET);
        innerFlame.setStroke(null);

        Polygon leftFin = new Polygon(62, 170, 18, 258, 66, 240);
        leftFin.setFill(Palette.SKY);
        Polygon rightFin = new Polygon(138, 170, 182, 258, 134, 240);
        rightFin.setFill(Palette.SKY);

        SVGPath body = new SVGPath();
        body.setContent("M100 8 C150 58 156 170 136 240 L64 240 C44 170 50 58 100 8 Z");
        body.setFill(Palette.PAPER);

        SVGPath nose = new SVGPath();
        nose.setContent("M100 8 C122 30 136 58 143 90 L57 90 C64 58 78 30 100 8 Z");
        nose.setFill(Palette.ROCKET);

        Circle window = new Circle(100, 138, 25);
        window.setFill(Palette.SKY);

        Circle shine = new Circle(91, 129, 7);
        shine.setFill(Palette.STAR);
        shine.setStroke(null);

        for (Shape outlined : new Shape[] {flame, leftFin, rightFin, body, nose, window}) {
            outlined.setStroke(Palette.INK);
            outlined.setStrokeWidth(Palette.OUTLINE);
            outlined.setStrokeLineJoin(StrokeLineJoin.ROUND);
        }

        Group rocket = new Group(flame, innerFlame, leftFin, rightFin, body, nose, window, shine);
        double scale = height / DESIGN_HEIGHT;
        rocket.setScaleX(scale);
        rocket.setScaleY(scale);
        // A Group wrapper makes layout use the scaled size instead of the design size.
        return new Group(rocket);
    }
}
