package com.liftoff.render3d;

import com.liftoff.render.Theme;
import com.liftoff.session.stub.Mission;
import javafx.scene.Group;
import javafx.scene.PointLight;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SplittableRandom;

/**
 * The cutscene at the end of each mission. Nova watches from the observation deck as the
 * spacecraft reaches the destination. When the mission worked, glowing data pulses stream back
 * and Nova cheers; when it did not, the ship's lights blink red and Nova scratches their head
 * before turning to the player to try again.
 */
public final class ArrivalScene extends CutsceneView {

    public static final double LENGTH = 8.6;
    private static final double[] PLANET = {6, -4.2, 15};
    private static final double PLANET_RADIUS = 4.2;
    private static final double ORBIT = 5.6;

    private final boolean success;
    private final RoundFigure nova = new RoundFigure(RoundFigure.Look.KID_NOVA);
    private final Sphere planet;
    private final Group ship = new Group();
    private final Box[] lights = new Box[2];
    private final javafx.scene.paint.PhongMaterial cyanLight = ModelLibrary.lit("#22E3F2", "#22E3F2");
    private final javafx.scene.paint.PhongMaterial redLight = ModelLibrary.lit("#FF4D4D", "#FF4D4D");
    private final List<Sphere> pulses = new ArrayList<>();
    private final List<Box> sparkles = new ArrayList<>();
    private final List<double[]> sparklePaths = new ArrayList<>();

    public ArrivalScene(Mission.Destination destination, boolean success) {
        super(Theme.NAVY_950, Color.web("#2A3352"));
        this.success = success;
        var random = new SplittableRandom(destination.ordinal() + 31);
        for (int i = 0; i < 220; i++) {
            double theta = random.nextDouble(Math.TAU);
            double phi = Math.acos(random.nextDouble(-1, 1));
            var star = glow(random.nextDouble(.06, .16), i % 9 == 0 ? "#FFE29A" : "#DCE8F7");
            at(star, 70 * Math.sin(phi) * Math.cos(theta), 70 * Math.cos(phi), 70 * Math.sin(phi) * Math.sin(theta));
            world.getChildren().add(star);
        }
        planet = globe(destination.name().toLowerCase(Locale.ROOT), switch (destination) {
            case EARTH -> "#3E7FD0";
            case MOON -> "#B9BCC4";
            case MARS -> "#C1552F";
        });
        at(planet, PLANET[0], PLANET[1], PLANET[2]);
        planet.setRotationAxis(Rotate.Y_AXIS);
        world.getChildren().add(planet);

        // The spacecraft: white body, gold foil, blue solar wings, an orange stripe and two lights.
        ship.getChildren().addAll(
                box(.7, .55, .9, 0, 0, 0, "#F2F5FA"),
                box(.72, .12, .92, 0, .1, 0, "#FF7A1A"),
                box(.5, .4, .3, 0, 0, .55, "#D9AE5E"),
                box(1.5, .05, .55, -1.15, 0, 0, "#2F64CF"),
                box(1.5, .05, .55, 1.15, 0, 0, "#2F64CF"));
        for (int side = 0; side < 2; side++) {
            lights[side] = new Box(.1, .1, .1);
            lights[side].setMaterial(cyanLight);
            at(lights[side], side == 0 ? -.3 : .3, -.32, -.2);
            ship.getChildren().add(lights[side]);
        }
        var dish = new Cylinder(.22, .05, 24);
        dish.setMaterial(ModelLibrary.lit("#22E3F2", "#0B7D8C"));
        at(dish, 0, -.32, 0);
        ship.getChildren().add(dish);
        world.getChildren().add(ship);

        // Data pulses that travel from the ship back to Nova.
        for (int i = 0; i < 6; i++) {
            var pulse = glow(.16, "#22E3F2");
            pulse.setVisible(false);
            pulses.add(pulse);
            world.getChildren().add(pulse);
        }
        // Star sparkles around Nova when it works.
        String[] colours = {"#FFCB69", "#22E3F2", "#FF7A1A", "#F2F5FA", "#8BE07A"};
        for (int i = 0; i < 60; i++) {
            var sparkle = new Box(.1, .1, .02);
            sparkle.setMaterial(ModelLibrary.lit(colours[i % colours.length], colours[i % colours.length]));
            sparkle.setVisible(false);
            sparkles.add(sparkle);
            sparklePaths.add(new double[] {random.nextDouble(Math.TAU), random.nextDouble(.6, 2.4),
                    random.nextDouble(-3.4, -.6), random.nextDouble(0, 1.2)});
            world.getChildren().add(sparkle);
        }

        var deck = new Cylinder(2.3, .3, 48);
        deck.setMaterial(ModelLibrary.surface("#16305E"));
        at(deck, 0, .15, 0);
        var rim = Meshes.view(Meshes.torus(2.3, .07, 64, 10), ModelLibrary.lit("#22E3F2", "#0B7D8C"));
        at(rim, 0, .02, 0);
        world.getChildren().addAll(deck, rim, nova);
        nova.setRotationAxis(Rotate.Y_AXIS);

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
        planet.setRotate(-20 + t * 5);

        // The ship flies out from beside the deck and settles into orbit around the destination.
        double travel = phase(t, .2, 2.6);
        double orbit = (t - 2.8) * .45;
        double[] start = {1.6, -3.2, 1.5};
        double[] parked = {PLANET[0] + Math.cos(Math.PI * .8 + Math.max(0, orbit)) * ORBIT, PLANET[1] - 1.6,
                PLANET[2] - Math.sin(Math.PI * .8 + Math.max(0, orbit)) * ORBIT};
        at(ship, lerp(start[0], parked[0], travel), lerp(start[1], parked[1], travel) - Math.sin(travel * Math.PI) * 1.5,
                lerp(start[2], parked[2], travel));
        ship.setRotationAxis(Rotate.Y_AXIS);
        ship.setRotate(t * 18);
        double scale = lerp(1, .8, travel);
        ship.setScaleX(scale);
        ship.setScaleY(scale);
        ship.setScaleZ(scale);
        boolean blink = !success && t > 3.0 && ((int) (t * 4)) % 2 == 0;
        for (var light : lights) {
            light.setMaterial(blink ? redLight : cyanLight);
        }

        // Data comes home: pulses run from the ship to the deck, one after another.
        for (int i = 0; i < pulses.size(); i++) {
            var pulse = pulses.get(i);
            double p = success ? (t - 3.0 - i * .35) / 1.4 : -1;
            pulse.setVisible(p > 0 && p < 1);
            if (pulse.isVisible()) {
                double[] from = {ship.getTranslateX(), ship.getTranslateY(), ship.getTranslateZ()};
                double[] to = {0, -2.4, 0};
                at(pulse, lerp(from[0], to[0], p), lerp(from[1], to[1], p) - Math.sin(p * Math.PI) * .8,
                        lerp(from[2], to[2], p));
            }
        }

        // Nova watches the ship, then turns to the player: cheering, or scratching their head.
        double toPlayer = phase(t, 4.3, .5);
        double watching = Math.toDegrees(Math.atan2(-(ship.getTranslateX()), -(ship.getTranslateZ())));
        nova.setRotate(lerpAngle(t < .2 ? 160 : watching, 18, toPlayer));
        nova.bob(t, false);
        if (success && t > 4.5) {
            nova.cheer(t);
            nova.body.setTranslateY(-Math.abs(Math.sin(t * 6)) * .15);
        } else if (!success && t > 3.4 && t < 6.0) {
            nova.wave(.2);
        } else if (!success && t >= 6.0) {
            nova.point();
        }

        for (int i = 0; i < sparkles.size(); i++) {
            var sparkle = sparkles.get(i);
            double[] path = sparklePaths.get(i);
            double since = t - 4.6 - path[3];
            sparkle.setVisible(success && since > 0 && since < 2.4);
            if (sparkle.isVisible()) {
                double r = path[1] * (.4 + since * .5);
                at(sparkle, Math.cos(path[0]) * r, path[2] - since * .6, Math.sin(path[0]) * r - .5);
                sparkle.setRotationAxis(Rotate.Z_AXIS);
                sparkle.setRotate(since * 400 + i * 30);
            }
        }

        double front = phase(t, 3.8, 1.2);
        double[] behind = {-3.2, -3.0, -6.6, 3.5, -3.0, 10};
        double[] onNova = {-.9, -2.1, -5.2, .2, -1.6, 0};
        double[] shot = new double[6];
        for (int i = 0; i < 6; i++) {
            shot[i] = lerp(behind[i], onNova[i], front);
        }
        look(shot[0], shot[1], shot[2], shot[3], shot[4], shot[5]);
    }

    private static Sphere globe(String name, String colour) {
        var sphere = new Sphere(PLANET_RADIUS, 96);
        var material = ModelLibrary.surface(colour);
        var resource = ArrivalScene.class.getResource("/com/liftoff/art/flight/" + name + "-map.jpg");
        if (resource != null) {
            material.setDiffuseColor(Color.WHITE);
            material.setDiffuseMap(new Image(resource.toExternalForm()));
        }
        material.setSpecularColor(Color.TRANSPARENT);
        sphere.setMaterial(material);
        return sphere;
    }
}
