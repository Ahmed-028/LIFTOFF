package com.liftoff.render3d;

import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.List;


final class SpaceShips {
    private SpaceShips() {
    }


    record Craft(Group node, List<Sphere> flames) {
        void flicker(double time, double throttle) {
            for (int i = 0; i < flames.size(); i++) {
                var flame = flames.get(i);
                double pulse = 1 + .18 * Math.sin(time * 31 + i * 1.7) + .1 * Math.sin(time * 53 + i);
                flame.setScaleY(Math.max(.05, throttle) * pulse * (1.6 + i * .55));
                flame.setVisible(throttle > .02);
            }
        }
    }


    static Craft capsuleShip(String bodyHex, String bandHex) {
        var root = new Group();
        var hull = new Sphere(1, 48);
        hull.setMaterial(shiny(bodyHex));
        var band = Meshes.view(Meshes.torus(1.0, .13, 64, 14), shiny(bandHex));
        var upperStripe = Meshes.view(Meshes.torus(.80, .05, 56, 10), shiny(bandHex));
        upperStripe.setTranslateY(-.6);
        root.getChildren().addAll(hull, band, upperStripe);
        for (int i = 0; i < 10; i++) {
            double angle = Math.TAU * i / 10;
            var window = new Sphere(.105, 16);
            window.setMaterial(ModelLibrary.lit("#7FF3FF", "#1BA7C9"));
            window.setScaleY(.8);
            Meshes.at(window, Math.cos(angle) * 1.11, 0, Math.sin(angle) * 1.11);
            root.getChildren().add(window);
        }
        var hatch = new Sphere(.32, 24);
        hatch.setScaleY(.45);
        hatch.setTranslateY(-.96);
        hatch.setMaterial(shiny(bandHex));
        var mast = new Cylinder(.025, .45);
        mast.setTranslateY(-1.25);
        mast.setMaterial(ModelLibrary.surface("#B9C3CF"));
        var beacon = new Sphere(.07, 12);
        beacon.setTranslateY(-1.5);
        beacon.setMaterial(ModelLibrary.lit("#FF5A5A", "#D41F1F"));
        root.getChildren().addAll(hatch, mast, beacon);

        var bell = Meshes.view(Meshes.lathe(new double[]{.28, .86, .34, .98, .46, 1.14, .54, 1.24}, 32),
                ModelLibrary.surface("#3D4656"));
        root.getChildren().add(bell);
        for (int i = 0; i < 4; i++) {
            double angle = Math.TAU * i / 4 + Math.PI / 4;
            double cx = Math.cos(angle);
            double cz = Math.sin(angle);
            var leg = new Cylinder(.045, .95);
            leg.setMaterial(ModelLibrary.surface("#AEB8C6"));
            Meshes.at(leg, cx * .98, .9, cz * .98);
            leg.getTransforms().add(new Rotate(-22, new javafx.geometry.Point3D(-cz, 0, cx)));
            var foot = new Cylinder(.17, .06);
            foot.setMaterial(ModelLibrary.surface("#5F6B7C"));
            Meshes.at(foot, cx * 1.16, 1.34, cz * 1.16);
            root.getChildren().addAll(leg, foot);
        }
        var flames = flames(root, 1.30, .40, "#FFF6C8", "#FFB347", "#FF6A2B");
        return new Craft(root, flames);
    }


    static Craft pod(String bodyHex, String windowHex) {
        var root = new Group();
        var hull = new Sphere(.62, 40);
        hull.setMaterial(shiny(bodyHex));
        var window = new Sphere(.48, 32);
        window.setScaleY(.42);
        window.setTranslateY(-.43);
        window.setMaterial(ModelLibrary.lit(windowHex, darker(windowHex)));
        var rim = Meshes.view(Meshes.torus(.47, .05, 40, 10), shiny("#5D6878"));
        rim.setTranslateY(-.44);
        var belt = Meshes.view(Meshes.torus(.62, .045, 48, 10), shiny("#5D6878"));
        belt.setTranslateY(.08);
        root.getChildren().addAll(hull, window, rim, belt);
        for (int i = 0; i < 3; i++) {
            double angle = Math.TAU * i / 3;
            var fin = new javafx.scene.shape.Box(.08, .36, .22);
            fin.setMaterial(ModelLibrary.surface("#7E8A9B"));
            Meshes.at(fin, Math.cos(angle) * .58, .38, Math.sin(angle) * .58);
            fin.setRotationAxis(Rotate.Y_AXIS);
            fin.setRotate(-Math.toDegrees(angle));
            root.getChildren().add(fin);
        }
        var flames = flames(root, .66, .22, "#E9FFFF", "#7FE8FF", "#3B8CFF");
        return new Craft(root, flames);
    }

    private static List<Sphere> flames(Group root, double y, double radius, String... colors) {
        var flames = new ArrayList<Sphere>();
        for (int i = 0; i < colors.length; i++) {
            var flame = new Sphere(radius * (1 - i * .18), 18);
            flame.setMaterial(ModelLibrary.lit(colors[i], colors[i]));
            flame.setTranslateY(y + radius * (.7 + i * .9));
            flame.setOpacity(.95 - i * .2);
            root.getChildren().add(flame);
            flames.add(flame);
        }
        return flames;
    }


    static MeshView asteroid(long seed, double size) {
        var rock = Meshes.view(Meshes.rock(seed, 10), ModelLibrary.surface(seed % 2 == 0 ? "#7A6E66" : "#8D8378"));
        rock.setScaleX(size);
        rock.setScaleY(size);
        rock.setScaleZ(size);
        return rock;
    }


    static Group ringedPlanet(double radius) {
        var planet = new Sphere(radius, 64);
        var material = new PhongMaterial(Color.WHITE);
        material.setDiffuseMap(stripes());
        material.setSpecularColor(Color.TRANSPARENT);
        planet.setMaterial(material);
        var ring = Meshes.view(Meshes.torus(radius * 1.75, radius * .45, 96, 12), ModelLibrary.lit("#E9D6A8", "#5E5240"));
        ring.setScaleY(.035);
        ring.setCullFace(javafx.scene.shape.CullFace.NONE);
        var innerRing = Meshes.view(Meshes.torus(radius * 1.22, radius * .08, 96, 10), ModelLibrary.lit("#CDB98F", "#4A4030"));
        innerRing.setScaleY(.05);
        var group = new Group(planet, ring, innerRing);
        group.getTransforms().addAll(new Rotate(-16, Rotate.Z_AXIS), new Rotate(14, Rotate.X_AXIS));
        return group;
    }

    private static Image stripes() {
        int height = 128;
        var image = new WritableImage(4, height);
        var writer = image.getPixelWriter();
        Color[] palette = {Color.web("#F2C89B"), Color.web("#E7A877"), Color.web("#F7E3C4"), Color.web("#D98E6A"),
                Color.web("#EBD0A7")};
        for (int y = 0; y < height; y++) {
            double t = y / (double) height;
            double band = (Math.sin(t * 34) + Math.sin(t * 13 + 1.2) * .6 + 1.6) / 3.2;
            Color a = palette[(int) (t * palette.length * 1.9) % palette.length];
            Color b = palette[((int) (t * palette.length * 1.9) + 2) % palette.length];
            Color color = a.interpolate(b, Math.clamp(band, 0, 1));
            for (int x = 0; x < 4; x++) {
                writer.setColor(x, y, color);
            }
        }
        return image;
    }

    static PhongMaterial shiny(String hex) {
        var material = new PhongMaterial(Color.web(hex));
        material.setSpecularColor(Color.web("#FFFFFF").deriveColor(0, 1, .55, 1));
        material.setSpecularPower(38);
        return material;
    }

    private static String darker(String hex) {
        var color = Color.web(hex).darker();
        return String.format("#%02X%02X%02X", (int) (color.getRed() * 255), (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}
