package com.liftoff.render;

import com.liftoff.session.stub.Mission;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

import java.util.List;

/**
 * The Star Match tiles. Each mission plays with its own set (farms and satellites for Earth,
 * landers and helmets for the Moon, rovers and drones for Mars), and every tile has its own
 * shape as well as its own colour, so they can be told apart without colour. Drawn on a
 * 100-unit grid centred on 0 and scaled to the size asked for.
 */
public final class SpaceTiles {

    private SpaceTiles() {
    }

    /** The six tile names for a destination, easiest first (easy games use the first five). */
    public static List<String> set(Mission.Destination destination) {
        return switch (destination) {
            case EARTH -> List.of("earth", "satellite", "leaf", "rocket", "star", "moon");
            case MOON -> List.of("moon", "lander", "helmet", "earth", "rocket", "star");
            case MARS -> List.of("mars", "rover", "drone", "rock", "rocket", "star");
        };
    }

    public static Node icon(String name, double size) {
        Group art = switch (name) {
            case "earth" -> earth();
            case "satellite" -> satellite();
            case "leaf" -> leaf();
            case "rocket" -> rocket();
            case "moon" -> moon();
            case "lander" -> lander();
            case "helmet" -> helmet();
            case "mars" -> mars();
            case "rover" -> rover();
            case "drone" -> drone();
            case "rock" -> rock();
            default -> star();
        };
        art.setScaleX(size / 100);
        art.setScaleY(size / 100);
        return new Group(art);
    }

    private static Group earth() {
        var ball = new Circle(0, 0, 36, Color.web("#2F7FD6"));
        var land1 = new Polygon(-26, -14, -8, -24, 2, -10, -6, 4, -20, 6);
        var land2 = new Polygon(8, 2, 26, -6, 30, 12, 14, 26, 4, 16);
        for (var land : new Polygon[] {land1, land2}) {
            land.setFill(Color.web("#4CB050"));
            land.setStroke(null);
        }
        var cloud = new Ellipse(-6, 22, 14, 5);
        cloud.setFill(Color.web("#FFFFFF", .85));
        Shape outline = new Circle(0, 0, 36, Color.TRANSPARENT);
        outline(outline, Theme.NAVY_950, 5);
        var group = new Group(ball, land1, land2, cloud);
        group.setClip(new Circle(0, 0, 36));
        return new Group(group, outline);
    }

    private static Group moon() {
        var ball = new Circle(0, 0, 34, Color.web("#D9DEEC"));
        outline(ball, Theme.NAVY_950, 5);
        var c1 = new Circle(-12, -8, 8, Color.web("#AEB6CC"));
        var c2 = new Circle(12, 10, 6, Color.web("#AEB6CC"));
        var c3 = new Circle(8, -16, 4, Color.web("#AEB6CC"));
        return new Group(ball, c1, c2, c3);
    }

    private static Group mars() {
        var ball = new Circle(0, 0, 36, Color.web("#D2643A"));
        var patch1 = new Ellipse(-10, 4, 16, 8);
        var patch2 = new Ellipse(14, -12, 10, 6);
        for (var patch : new Ellipse[] {patch1, patch2}) {
            patch.setFill(Color.web("#8E3B22"));
        }
        var cap = new Ellipse(0, -32, 14, 6);
        cap.setFill(Color.WHITE);
        var group = new Group(ball, patch1, patch2, cap);
        group.setClip(new Circle(0, 0, 36));
        Shape outline = new Circle(0, 0, 36, Color.TRANSPARENT);
        outline(outline, Theme.NAVY_950, 5);
        return new Group(group, outline);
    }

    private static Group star() {
        var star = new Polygon();
        for (int i = 0; i < 10; i++) {
            double r = i % 2 == 0 ? 42 : 19;
            double angle = Math.PI / 5 * i - Math.PI / 2;
            star.getPoints().addAll(Math.cos(angle) * r, Math.sin(angle) * r + 3);
        }
        star.setFill(Theme.AMBER);
        outline(star, Theme.NAVY_950, 5);
        return new Group(star, new Circle(-8, -4, 5, Color.WHITE));
    }

    private static Group rocket() {
        var body = new Polygon(0, -44, 18, -18, 18, 26, -18, 26, -18, -18);
        body.setFill(Theme.HULL_WHITE);
        outline(body, Theme.NAVY_950, 5);
        var nose = new Polygon(0, -44, 13, -26, -13, -26);
        nose.setFill(Color.web("#FF6F91"));
        var left = new Polygon(-18, 6, -34, 34, -18, 26);
        var right = new Polygon(18, 6, 34, 34, 18, 26);
        for (var fin : new Polygon[] {left, right}) {
            fin.setFill(Color.web("#FF6F91"));
            outline(fin, Theme.NAVY_950, 4);
        }
        var window = new Circle(0, -4, 8, Theme.CYAN);
        outline(window, Theme.NAVY_950, 3);
        var flame = new Polygon(-10, 28, 0, 46, 10, 28);
        flame.setFill(Theme.AMBER);
        return new Group(flame, left, right, body, nose, window);
    }

    private static Group satellite() {
        var left = new Rectangle(-46, -12, 28, 24);
        var right = new Rectangle(18, -12, 28, 24);
        for (var panel : new Rectangle[] {left, right}) {
            panel.setFill(Color.web("#5B8CFF"));
            outline(panel, Theme.NAVY_950, 4);
        }
        var body = new Rectangle(-16, -18, 32, 36);
        body.setArcWidth(10);
        body.setArcHeight(10);
        body.setFill(Color.web("#D9AE5E"));
        outline(body, Theme.NAVY_950, 5);
        var dish = new Circle(0, -26, 9, Theme.HULL_WHITE);
        outline(dish, Theme.NAVY_950, 4);
        return new Group(left, right, dish, body);
    }

    /** A crop leaf, for the farms Farm Watch looks after. */
    private static Group leaf() {
        var leaf = new Ellipse(4, -4, 22, 36);
        leaf.setRotate(35);
        leaf.setFill(Color.web("#6BCB4F"));
        outline(leaf, Theme.NAVY_950, 5);
        var vein = new Line(-16, 26, 18, -26);
        vein.setStroke(Color.web("#2F7A2A"));
        vein.setStrokeWidth(4);
        vein.setStrokeLineCap(StrokeLineCap.ROUND);
        var stem = new Line(-16, 26, -24, 38);
        stem.setStroke(Theme.NAVY_950);
        stem.setStrokeWidth(5);
        stem.setStrokeLineCap(StrokeLineCap.ROUND);
        return new Group(stem, leaf, vein);
    }

    /** A Moon lander: gold body on four legs. */
    private static Group lander() {
        var group = new Group();
        for (int side : new int[] {-1, 1}) {
            var leg = new Line(side * 14, 6, side * 34, 34);
            leg.setStroke(Theme.NAVY_950);
            leg.setStrokeWidth(5);
            leg.setStrokeLineCap(StrokeLineCap.ROUND);
            var foot = new Rectangle(side * 34 - 8, 32, 16, 6);
            foot.setFill(Theme.NAVY_950);
            group.getChildren().addAll(leg, foot);
        }
        var body = new Polygon(-22, 10, -16, -16, 16, -16, 22, 10);
        body.setFill(Color.web("#E2B34C"));
        outline(body, Theme.NAVY_950, 5);
        var top = new Rectangle(-12, -34, 24, 18);
        top.setFill(Theme.HULL_WHITE);
        outline(top, Theme.NAVY_950, 4);
        var window = new Rectangle(-6, -30, 12, 8);
        window.setFill(Theme.CYAN);
        group.getChildren().addAll(body, top, window);
        return group;
    }

    /** An astronaut helmet with a gold visor. */
    private static Group helmet() {
        var shell = new Circle(0, 0, 36, Theme.HULL_WHITE);
        outline(shell, Theme.NAVY_950, 5);
        var visor = new Ellipse(0, 2, 24, 18);
        visor.setFill(Color.web("#C99A2E"));
        outline(visor, Theme.NAVY_950, 4);
        var shine = new Ellipse(-8, -4, 7, 4);
        shine.setFill(Color.web("#FFFFFF", .8));
        var light = new Circle(26, -20, 5, Theme.ORANGE);
        return new Group(shell, visor, shine, light);
    }

    /** A Mars rover: a body on six wheels with a camera mast. */
    private static Group rover() {
        var group = new Group();
        for (int i = -1; i <= 1; i++) {
            var wheel = new Circle(i * 24, 24, 10, Color.web("#3D4E70"));
            outline(wheel, Theme.NAVY_950, 4);
            group.getChildren().add(wheel);
        }
        var body = new Rectangle(-36, -6, 72, 22);
        body.setArcWidth(8);
        body.setArcHeight(8);
        body.setFill(Theme.HULL_WHITE);
        outline(body, Theme.NAVY_950, 5);
        var mast = new Line(18, -6, 18, -30);
        mast.setStroke(Theme.NAVY_950);
        mast.setStrokeWidth(5);
        var head = new Rectangle(8, -40, 20, 12);
        head.setFill(Color.web("#D9AE5E"));
        outline(head, Theme.NAVY_950, 4);
        var eye = new Circle(14, -34, 3, Theme.CYAN);
        var panel = new Rectangle(-30, -14, 40, 8);
        panel.setFill(Color.web("#5B8CFF"));
        outline(panel, Theme.NAVY_950, 3);
        group.getChildren().addAll(body, panel, mast, head, eye);
        return group;
    }

    /** A Mars helicopter drone: two long rotor blades over a small body on legs. */
    private static Group drone() {
        var group = new Group();
        for (int side : new int[] {-1, 1}) {
            var leg = new Line(side * 8, 12, side * 22, 32);
            leg.setStroke(Theme.NAVY_950);
            leg.setStrokeWidth(4);
            leg.setStrokeLineCap(StrokeLineCap.ROUND);
            group.getChildren().add(leg);
        }
        var body = new Rectangle(-14, -4, 28, 20);
        body.setArcWidth(8);
        body.setArcHeight(8);
        body.setFill(Color.web("#B388FF"));
        outline(body, Theme.NAVY_950, 4);
        var mast = new Line(0, -4, 0, -24);
        mast.setStroke(Theme.NAVY_950);
        mast.setStrokeWidth(4);
        var blade1 = new Rectangle(-42, -30, 84, 6);
        var blade2 = new Rectangle(-36, -20, 72, 6);
        for (var blade : new Rectangle[] {blade1, blade2}) {
            blade.setArcWidth(6);
            blade.setArcHeight(6);
            blade.setFill(Theme.HULL_WHITE);
            outline(blade, Theme.NAVY_950, 3);
        }
        group.getChildren().addAll(body, mast, blade2, blade1);
        return group;
    }

    /** A red Mars rock. */
    private static Group rock() {
        var rock = new Polygon(-34, 18, -28, -10, -8, -26, 18, -22, 34, -2, 30, 22, 4, 30);
        rock.setFill(Color.web("#A0522D"));
        outline(rock, Theme.NAVY_950, 5);
        var shade = new Polygon(4, 30, 30, 22, 34, -2, 12, 10);
        shade.setFill(Color.web("#6B2E1A"));
        var dot1 = new Circle(-12, -6, 5, Color.web("#C9784E"));
        var dot2 = new Circle(-2, 12, 3, Color.web("#C9784E"));
        return new Group(rock, shade, dot1, dot2);
    }

    private static void outline(Shape shape, Color colour, double width) {
        shape.setStroke(colour);
        shape.setStrokeWidth(width);
        shape.setStrokeLineJoin(StrokeLineJoin.ROUND);
    }
}
