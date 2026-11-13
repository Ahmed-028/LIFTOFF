package com.liftoff.render;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;

/** Small drawn icons, each on a 24 x 24 grid. Always shown next to a text label. */
public final class Icons {

    private static final double DESIGN_SIZE = 24;
    private static final double OUTLINE = 2.5;

    private Icons() {
    }

    /** Budget: a coin. */
    public static Node coin(double size) {
        Circle rim = disc(Palette.BUDGET);
        Text dollar = new Text("$");
        dollar.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        dollar.setFill(Palette.INK);
        dollar.setBoundsType(TextBoundsType.VISUAL);
        dollar.setX(12 - dollar.getLayoutBounds().getWidth() / 2 - dollar.getLayoutBounds().getMinX());
        dollar.setY(12 + dollar.getLayoutBounds().getHeight() / 2 - dollar.getLayoutBounds().getMaxY());
        return scaled(new Group(rim, dollar), size);
    }

    /** Time: a clock. */
    public static Node clock(double size) {
        Circle face = disc(Palette.TIME);
        Line hourHand = stroke(new Line(12, 12, 12, 6.5));
        Line minuteHand = stroke(new Line(12, 12, 16, 14));
        return scaled(new Group(face, hourHand, minuteHand), size);
    }

    /** Chosen: a tick. */
    public static Node tick(double size) {
        Circle badge = disc(Palette.SKY);
        Polyline mark = stroke(new Polyline(7, 12.5, 10.5, 16, 17, 8.5));
        mark.setFill(Color.TRANSPARENT);
        return scaled(new Group(badge, mark), size);
    }

    private static Circle disc(Color fill) {
        Circle disc = new Circle(12, 12, 10, fill);
        disc.setStroke(Palette.INK);
        disc.setStrokeWidth(OUTLINE);
        return disc;
    }

    private static <S extends Shape> S stroke(S shape) {
        shape.setStroke(Palette.INK);
        shape.setStrokeWidth(OUTLINE);
        shape.setStrokeLineCap(StrokeLineCap.ROUND);
        shape.setStrokeLineJoin(StrokeLineJoin.ROUND);
        return shape;
    }

    private static Node scaled(Group icon, double size) {
        double scale = size / DESIGN_SIZE;
        icon.setScaleX(scale);
        icon.setScaleY(scale);
        return new Group(icon);
    }
}
