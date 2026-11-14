package com.liftoff.render3d;

import com.liftoff.render.Theme;
import com.liftoff.session.stub.Mission;
import javafx.scene.Group;
import javafx.scene.PointLight;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;

import java.util.Locale;
import java.util.SplittableRandom;

/**
 * The cutscene before a mission: Nova stands on a little observation deck in space looking at
 * the destination (the real NASA map of Earth, the Moon or Mars on a globe), turns to wave at
 * the player and points the way, and the camera flies towards the planet.
 */
public final class MissionScene extends CutsceneView {

    public static final double LENGTH = 8.2;
    private static final double[] PLANET = {6, -4.2, 15};
    private static final double PLANET_RADIUS = 4.2;

    private final RoundFigure nova = new RoundFigure(RoundFigure.Look.KID_NOVA);
    private final Sphere planet;
    private final Group satellite = new Group();
    private final double facingPlanet;

    public MissionScene(Mission.Destination destination) {
        super(Theme.NAVY_950, Color.web("#2A3352"));
        var random = new SplittableRandom(destination.ordinal() + 9);
        for (int i = 0; i < 220; i++) {
            double theta = random.nextDouble(Math.TAU);
            double phi = Math.acos(random.nextDouble(-1, 1));
            double r = 70;
            var star = glow(random.nextDouble(.06, .16), i % 9 == 0 ? "#FFE29A" : "#DCE8F7");
            at(star, r * Math.sin(phi) * Math.cos(theta), r * Math.cos(phi), r * Math.sin(phi) * Math.sin(theta));
            world.getChildren().add(star);
        }

        String name = destination.name().toLowerCase(Locale.ROOT);
        planet = globe(name, PLANET_RADIUS, switch (destination) {
            case EARTH -> "#3E7FD0";
            case MOON -> "#B9BCC4";
            case MARS -> "#C1552F";
        });
        at(planet, PLANET[0], PLANET[1], PLANET[2]);
        planet.setRotationAxis(Rotate.Y_AXIS);
        world.getChildren().add(planet);
        switch (destination) {
            case EARTH -> world.getChildren().add(at(globe("moon", 1.1, "#B9BCC4"), -14, -9, 42));
            case MOON -> world.getChildren().add(at(globe("earth", 1.6, "#3E7FD0"), -15, -10, 45));
            case MARS -> {
                var phobos = Meshes.view(Meshes.rock(5, 2), ModelLibrary.surface("#7A6A5E"));
                phobos.setScaleX(.7);
                phobos.setScaleY(.5);
                phobos.setScaleZ(.6);
                world.getChildren().add(at(phobos, 1.2, -8.4, 11));
            }
        }

        // A little spacecraft circling the destination.
        var core = box(.5, .5, .7, 0, 0, 0, "#D9AE5E");
        var leftPanel = box(1.1, .04, .5, -.85, 0, 0, "#2F64CF");
        var rightPanel = box(1.1, .04, .5, .85, 0, 0, "#2F64CF");
        var dish = glow(.12, "#22E3F2");
        at(dish, 0, -.32, 0);
        satellite.getChildren().addAll(core, leftPanel, rightPanel, dish);
        world.getChildren().add(satellite);

        // Nova's observation deck.
        var deck = new Cylinder(2.3, .3, 48);
        deck.setMaterial(ModelLibrary.surface("#16305E"));
        at(deck, 0, .15, 0);
        var rim = Meshes.view(Meshes.torus(2.3, .07, 64, 10), ModelLibrary.lit("#22E3F2", "#0B7D8C"));
        at(rim, 0, .02, 0);
        var stripe = Meshes.view(Meshes.torus(1.6, .05, 64, 8), ModelLibrary.lit("#FF7A1A", "#7A2E05"));
        at(stripe, 0, -.01, 0);
        world.getChildren().addAll(deck, rim, stripe, nova);
        nova.setRotationAxis(Rotate.Y_AXIS);
        facingPlanet = Math.toDegrees(Math.atan2(-PLANET[0], -PLANET[2]));

        var sun = new PointLight(Color.web("#FFF4E0"));
        at(sun, -30, -18, -12);
        var fill = new PointLight(Color.web("#3A5A9E"));
        at(fill, 8, -4, -10);
        world.getChildren().addAll(sun, fill);
        update(0);
    }

    @Override
    public double length() {
        return LENGTH;
    }

    @Override
    public void update(double t) {
        planet.setRotate(-30 + t * 6);
        double orbit = t * .55;
        at(satellite, PLANET[0] + Math.cos(orbit) * 6.2, PLANET[1] - 1.4 + Math.sin(orbit) * 1.2,
                PLANET[2] - Math.sin(orbit) * 6.2);
        satellite.setRotationAxis(Rotate.Y_AXIS);
        satellite.setRotate(Math.toDegrees(orbit));

        // Look at the planet, turn to wave hello, then point the way.
        double turn = phase(t, 2.4, .6);
        nova.setRotate(lerpAngle(facingPlanet, 22, turn));
        nova.bob(t, false);
        if (t > 3.0 && t < 4.7) {
            nova.wave(t - 3.0);
        } else if (t >= 4.7) {
            nova.point();
        }
        nova.body.setTranslateY(-Math.abs(Math.sin(t * 2.2)) * .04);

        double front = phase(t, 2.0, 1.2);
        double fly = phase(t, 5.2, 2.8);
        double[] behind = {-3.6, -2.8, -6.8, 3, -2.6, 10};
        double[] hello = {-.9, -2.1, -4.8, .2, -1.5, 0};
        double[] planetShot = {5.4, -7.6, 5.0, PLANET[0], PLANET[1], PLANET[2]};
        double[] shot = new double[6];
        for (int i = 0; i < 6; i++) {
            shot[i] = lerp(lerp(behind[i], hello[i], front), planetShot[i], fly);
        }
        look(shot[0], shot[1], shot[2], shot[3], shot[4], shot[5]);
    }

    private static Sphere globe(String name, double radius, String colour) {
        var sphere = new Sphere(radius, 96);
        var material = ModelLibrary.surface(colour);
        var resource = MissionScene.class.getResource("/com/liftoff/art/flight/" + name + "-map.jpg");
        if (resource != null) {
            material.setDiffuseColor(Color.WHITE);
            material.setDiffuseMap(new Image(resource.toExternalForm()));
        }
        material.setSpecularColor(Color.TRANSPARENT);
        sphere.setMaterial(material);
        return sphere;
    }
}
