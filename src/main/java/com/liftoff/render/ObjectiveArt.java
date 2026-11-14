package com.liftoff.render;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;


public final class ObjectiveArt {

    private static final double DESIGN_SIZE = 120;
    private static final double OUTLINE = 4;

    private ObjectiveArt() {
    }


    public static Node forObjective(String objectiveId, double size) {
        Group art = switch (objectiveId) {
            case "farm-watch" -> farm();
            case "storm-chasers" -> storm();
            case "ice-patrol" -> ice();
            default -> planet();
        };
        double scale = size / DESIGN_SIZE;
        art.setScaleX(scale);
        art.setScaleY(scale);
        return new Group(art);
    }

    private static Group farm() {
        Rectangle field = tile(Palette.BUDGET);
        Group rows = new Group();
        for (int x = 24; x <= 96; x += 24) {
            Line row = new Line(x, 62, x - 12, 108);
            row.setStroke(Color.web("#1F9E6B"));
            row.setStrokeWidth(6);
            row.setStrokeLineCap(StrokeLineCap.ROUND);
            rows.getChildren().add(row);
        }
        Rectangle sky = new Rectangle(8, 8, 104, 44);
        sky.setArcWidth(24);
        sky.setArcHeight(24);
        sky.setFill(Palette.SKY);
        Circle sun = outlined(new Circle(86, 30, 13), Palette.POWER);
        return new Group(field, sky, rows, sun, frame());
    }

    private static Group storm() {
        Rectangle night = tile(Palette.SPACE_500);
        Circle left = new Circle(42, 52, 20, Palette.STAR);
        Circle middle = new Circle(62, 42, 24, Palette.STAR);
        Circle right = new Circle(84, 54, 18, Palette.STAR);
        Rectangle base = new Rectangle(34, 50, 58, 22);
        base.setArcWidth(22);
        base.setArcHeight(22);
        base.setFill(Palette.STAR);
        Polygon bolt = outlined(new Polygon(64, 66, 50, 90, 62, 90, 54, 110, 78, 82, 66, 82, 74, 66), Palette.POWER);
        Group rain = new Group();
        for (int x = 34; x <= 94; x += 30) {
            Line drop = new Line(x, 82, x - 6, 98);
            drop.setStroke(Palette.SKY);
            drop.setStrokeWidth(5);
            drop.setStrokeLineCap(StrokeLineCap.ROUND);
            rain.getChildren().add(drop);
        }
        return new Group(night, rain, left, middle, right, base, bolt, frame());
    }

    private static Group ice() {
        Rectangle sky = tile(Palette.SKY);
        Rectangle sea = new Rectangle(8, 84, 104, 28);
        sea.setArcWidth(24);
        sea.setArcHeight(24);
        sea.setFill(Palette.SPACE_500);
        Polygon berg = outlined(new Polygon(22, 86, 44, 34, 60, 56, 76, 26, 100, 86), Palette.STAR);
        return new Group(sky, sea, berg, frame());
    }

    private static Group planet() {
        Rectangle space = tile(Palette.SPACE_500);
        Circle globe = outlined(new Circle(60, 60, 34), Palette.SKY);
        return new Group(space, globe, frame());
    }

    private static Rectangle tile(Color fill) {
        Rectangle tile = new Rectangle(8, 8, 104, 104);
        tile.setArcWidth(24);
        tile.setArcHeight(24);
        tile.setFill(fill);
        return tile;
    }

    private static Rectangle frame() {
        Rectangle frame = new Rectangle(8, 8, 104, 104);
        frame.setArcWidth(24);
        frame.setArcHeight(24);
        frame.setFill(Color.TRANSPARENT);
        frame.setStroke(Palette.INK);
        frame.setStrokeWidth(OUTLINE);
        return frame;
    }

    private static <S extends Shape> S outlined(S shape, Color fill) {
        shape.setFill(fill);
        shape.setStroke(Palette.INK);
        shape.setStrokeWidth(OUTLINE);
        shape.setStrokeLineJoin(StrokeLineJoin.ROUND);
        return shape;
    }
}
