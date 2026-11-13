package com.liftoff.render3d;

import javafx.application.Platform;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SplittableRandom;

/**
 * Interactive 3D capsule ship cabin with curved Capsule-style architecture, an astronaut rider
 * in a pilot seat, a central live telemetry console with holographic planet display, and a
 * panoramic front viewport looking out to the launch pad or deep space.
 */
public final class CabinView extends StackPane {
    private static final int SCREEN_W = 640;
    private static final int SCREEN_H = 300;

    private final boolean inFlight;
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final Rotate cameraYaw = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate cameraPitch = new Rotate(0, Rotate.X_AXIS);

    private final Group pilot = new Group();
    private final Group pilotHead = new Group();
    private final Group pilotTorso = new Group();
    private final Sphere[] stars = new Sphere[56];
    private final Box[] warpStreaks = new Box[18];
    private final List<Box> ceilingLamps = new ArrayList<>();
    private final List<Box> consoleButtons = new ArrayList<>();

    private final PointLight mainLight = new PointLight(Color.web("#DCEEFF"));
    private final PointLight consoleGlow = new PointLight(Color.web("#2CE8F5"));
    private final AmbientLight ambientLight = new AmbientLight(Color.web("#8896B5"));

    private final Sphere holoPlanet = new Sphere(.22, 24);
    private final MeshView holoRing;
    private final Sphere gravityRing;
    private final Group outsidePod;
    private final Sphere outsidePlanet = new Sphere(1.6, 36);

    private final Canvas screenCanvas = new Canvas(SCREEN_W, SCREEN_H);
    private final WritableImage screenImage = new WritableImage(SCREEN_W, SCREEN_H);
    private final PhongMaterial screenMaterial = new PhongMaterial(Color.WHITE);

    private String lastSignature = "";
    private double radarSweep;
    private double radarPingAlpha;
    private int lightMode;

    public CabinView(boolean inFlight) {
        this.inFlight = inFlight;
        var world = new Group();
        var outside = buildOutsideView(inFlight);
        outsidePod = buildOutsidePod(outside);
        var room = buildCapsuleRoom();
        holoRing = buildCentralConsole(room);
        gravityRing = buildGravityUnit(room);
        buildPilotAndSeat(room);

        camera.setNearClip(.08);
        camera.setFarClip(65);
        camera.setFieldOfView(58);
        camera.getTransforms().addAll(cameraYaw, cameraPitch);

        mainLight.setTranslateY(-2.1);
        mainLight.setTranslateZ(.6);
        consoleGlow.setTranslateY(-.2);
        consoleGlow.setTranslateZ(3.1);

        world.getChildren().addAll(outside, room, camera, mainLight, consoleGlow, ambientLight);

        var scene = new SubScene(world, 1, 1, true, SceneAntialiasing.BALANCED);
        scene.setMouseTransparent(true);
        scene.setCamera(camera);
        scene.setFill(Color.web(inFlight ? "#071022" : "#1D446E"));
        scene.widthProperty().bind(widthProperty());
        scene.heightProperty().bind(heightProperty());
        getChildren().add(scene);

        setLightMode(0);
        updateConsole("Earth -> Orbit", inFlight ? "Cruising" : "On the launch pad", "Standby",
                0, 0, 0, 0, 0, 0, 0, "1G (Normal)", "Systems initializing...", 0);
        station(0);
    }

    private Group buildOutsideView(boolean inFlight) {
        var outside = new Group();
        // Distant backdrop plate behind the window aperture.
        var backdrop = box(16, 12, .2, 0, -1.2, 18.5, inFlight ? "#050B1B" : "#4A90D6");
        backdrop.setMaterial(ModelLibrary.lit(inFlight ? "#060D20" : "#56A5EC", inFlight ? "#040918" : "#2D6FB0"));
        outside.getChildren().add(backdrop);

        if (inFlight) {
            outsidePlanet.setTranslateX(2.4);
            outsidePlanet.setTranslateY(-2.0);
            outsidePlanet.setTranslateZ(15.5);
            outsidePlanet.setMaterial(ModelLibrary.lit("#4F92D6", "#163B6B"));
            outside.getChildren().add(outsidePlanet);

            var random = new SplittableRandom(42);
            for (int i = 0; i < stars.length; i++) {
                double x = random.nextDouble(-5.5, 5.5);
                double y = random.nextDouble(-4.5, 2.2);
                double z = random.nextDouble(8.2, 17.0);
                var star = new Sphere(random.nextDouble(.022, .055), 8);
                star.setTranslateX(x);
                star.setTranslateY(y);
                star.setTranslateZ(z);
                star.setMaterial(ModelLibrary.lit(i % 5 == 0 ? "#FFD782" : "#D4F4FF", "#6AC8F0"));
                stars[i] = star;
                outside.getChildren().add(star);
            }
            for (int i = 0; i < warpStreaks.length; i++) {
                var streak = new Box(.028, .028, random.nextDouble(.7, 1.8));
                streak.setTranslateX(random.nextDouble(-3.6, 3.6));
                streak.setTranslateY(random.nextDouble(-3.2, .8));
                streak.setTranslateZ(random.nextDouble(7.5, 16.5));
                streak.setMaterial(ModelLibrary.lit("#9BF2FF", "#2DD5F2"));
                warpStreaks[i] = streak;
                outside.getChildren().add(streak);
            }
        } else {
            // Friendly launchpad gantry & sky clouds outside the pad window.
            var sun = new Sphere(.95, 24);
            sun.setTranslateX(-3.2);
            sun.setTranslateY(-3.3);
            sun.setTranslateZ(16.0);
            sun.setMaterial(ModelLibrary.lit("#FFF6CE", "#FFE07D"));
            outside.getChildren().add(sun);

            for (int i = 0; i < 4; i++) {
                var cloud = new Sphere(.65 + (i % 2) * .25, 16);
                cloud.setScaleX(2.1);
                cloud.setScaleY(.55);
                cloud.setTranslateX(-2.5 + i * 1.6);
                cloud.setTranslateY(-2.0 + (i % 2) * .5);
                cloud.setTranslateZ(15.0 + i * .3);
                cloud.setMaterial(ModelLibrary.lit("#F5FAFF", "#B8D6F5"));
                outside.getChildren().add(cloud);
            }

            // Red-and-white NASA style launch gantry tower on the left.
            for (int tier = 0; tier < 5; tier++) {
                double y = .8 - tier * 1.05;
                outside.getChildren().add(lamp(.22, .95, .22, -2.15, y, 11.0, tier % 2 == 0 ? "#E8543E" : "#F3F6FA"));
                outside.getChildren().add(lamp(1.3, .14, .14, -1.5, y - .3, 11.0, "#C7D2E0"));
            }
        }
        return outside;
    }

    private Group buildOutsidePod(Group outside) {
        if (!inFlight) {
            return null;
        }
        var craft = SpaceShips.pod("#F2F4F7", "#FF5277");
        var podGroup = new Group(craft.node());
        podGroup.getTransforms().addAll(new Scale(.55, .55, .55), new Rotate(-90, Rotate.X_AXIS));
        podGroup.setTranslateX(-2.3);
        podGroup.setTranslateY(-1.8);
        podGroup.setTranslateZ(11.8);
        outside.getChildren().add(podGroup);
        return podGroup;
    }

    private Group buildCapsuleRoom() {
        var room = new Group();
        // Floor deck + central walkway guide strip.
        room.getChildren().add(box(8.2, .24, 11.5, 0, 2.05, 1.0, "#2C3B50"));
        room.getChildren().add(box(1.9, .04, 8.6, 0, 1.91, -.4, "#1C293D"));
        room.getChildren().add(lamp(.08, .05, 8.6, -1.0, 1.91, -.4, "#24E0F0"));
        room.getChildren().add(lamp(.08, .05, 8.6, 1.0, 1.91, -.4, "#24E0F0"));
        room.getChildren().add(lamp(.26, .045, 8.2, 0, 1.91, -.4, "#F39C38"));

        // Ceiling & side capsule walls (rich shaded tones so lights never blow out to flat white).
        room.getChildren().addAll(
                box(8.2, .22, 11.5, 0, -3.05, 1.0, "#4B5D75"),
                box(.24, 5.2, 11.5, -4.0, -.5, 1.0, "#7C8FA8"),
                box(.24, 5.2, 11.5, 4.0, -.5, 1.0, "#7C8FA8"),
                box(8.2, 5.2, .24, 0, -.5, -4.8, "#3B4C63"));

        // Front bulkhead around the open circular viewport window (center 0, -1.20, 6.45).
        room.getChildren().addAll(
                box(2.45, 5.2, .28, -2.8, -.5, 6.45, "#677A94"),
                box(2.45, 5.2, .28, 2.8, -.5, 6.45, "#677A94"),
                box(3.3, 1.72, .28, 0, 1.18, 6.45, "#53657E"),
                box(3.3, .36, .28, 0, -2.89, 6.45, "#53657E"));

        // Curved ceiling shoulders + accent stripes (Capsule Corp color theme).
        for (int side : new int[]{-1, 1}) {
            var shoulder = box(2.3, .22, 11.2, side * 3.15, -2.35, 1.0, "#5C6F88");
            shoulder.setRotationAxis(Rotate.Z_AXIS);
            shoulder.setRotate(side * 38);
            var stripe = box(.09, .24, 11.0, side * 3.85, -.25, 1.0, "#E0872D");
            var blueTrim = box(.12, 1.35, 11.0, side * 3.85, 1.25, 1.0, "#1D324D");
            room.getChildren().addAll(shoulder, stripe, blueTrim);
        }

        // Capsule structural ribs + overhead light strips.
        for (int z = -3; z <= 5; z += 2) {
            for (int side : new int[]{-1, 1}) {
                room.getChildren().add(box(.28, 4.9, .26, 3.86 * side, -.5, z, "#43556D"));
                var lightStrip = lamp(.14, .10, 1.35, 3.55 * side, -2.35, z, "#E69E45");
                ceilingLamps.add(lightStrip);
                room.getChildren().add(lightStrip);
            }
        }

        // Giant circular capsule viewport frame around the front window opening.
        var outerRim = Meshes.view(Meshes.torus(1.94, .34, 64, 14), ModelLibrary.surface("#283C57"));
        outerRim.setRotationAxis(Rotate.X_AXIS);
        outerRim.setRotate(90);
        outerRim.setTranslateY(-1.20);
        outerRim.setTranslateZ(6.36);

        var cyanRim = Meshes.view(Meshes.torus(1.64, .055, 64, 10), ModelLibrary.lit("#2CE8F5", "#138599"));
        cyanRim.setRotationAxis(Rotate.X_AXIS);
        cyanRim.setRotate(90);
        cyanRim.setTranslateY(-1.20);
        cyanRim.setTranslateZ(6.18);

        room.getChildren().addAll(outerRim, cyanRim);
        return room;
    }

    private MeshView buildCentralConsole(Group room) {
        var console = new Group();
        // Main curved-look central console pedestal & desk.
        console.getChildren().addAll(
                box(3.7, 1.45, 1.15, 0, 1.22, 3.95, "#233246"),
                box(4.2, .22, 1.45, 0, .44, 3.85, "#364A63"),
                lamp(3.9, .05, .06, 0, .54, 3.12, "#24E0F0"));

        // Side angled wings on the console.
        for (int side : new int[]{-1, 1}) {
            var wing = box(1.1, .95, .9, side * 2.1, 1.45, 3.55, "#2B3D54");
            wing.setRotationAxis(Rotate.Y_AXIS);
            wing.setRotate(side * -24);
            var sideScreen = lamp(.75, .44, .06, side * 1.98, .12, 3.45, "#153D56");
            sideScreen.setRotationAxis(Rotate.Y_AXIS);
            sideScreen.setRotate(side * -24);
            console.getChildren().addAll(wing, sideScreen);
        }

        // Live central display screen using dynamic Canvas texture.
        screenMaterial.setDiffuseMap(screenImage);
        screenMaterial.setSelfIlluminationMap(screenImage);
        screenMaterial.setSpecularColor(Color.web("#8EEEF8"));
        screenMaterial.setSpecularPower(16);

        var screenFrame = box(2.85, 1.38, .10, 0, -.22, 3.68, "#1A2636");
        screenFrame.setRotationAxis(Rotate.X_AXIS);
        screenFrame.setRotate(-22);

        var screenMesh = Meshes.view(Meshes.quad(2.68, 1.24), screenMaterial);
        screenMesh.setCullFace(CullFace.NONE);
        screenMesh.setTranslateY(-.22);
        screenMesh.setTranslateZ(3.61);
        screenMesh.setRotationAxis(Rotate.X_AXIS);
        screenMesh.setRotate(-22);

        console.getChildren().addAll(screenFrame, screenMesh);

        // Glowing interactive-looking buttons & dual flight joysticks on the desk surface.
        String[] buttonColors = {"#24E0F0", "#FFD273", "#5DF29A", "#FF7B39", "#24E0F0", "#FFD273", "#5DF29A"};
        for (int i = -3; i <= 3; i++) {
            var btn = lamp(.18, .06, .12, i * .34, .31, 3.30, buttonColors[i + 3]);
            consoleButtons.add(btn);
            console.getChildren().add(btn);
        }
        // Flight joysticks + throttle handle.
        for (int side : new int[]{-1, 1}) {
            var base = new Cylinder(.14, .08, 16);
            base.setTranslateX(side * 1.35);
            base.setTranslateY(.30);
            base.setTranslateZ(3.38);
            base.setMaterial(ModelLibrary.surface("#182230"));

            var stick = new Cylinder(.045, .36, 12);
            stick.setTranslateX(side * 1.35);
            stick.setTranslateY(.12);
            stick.setTranslateZ(3.38);
            stick.setMaterial(ModelLibrary.surface("#8EA0B5"));

            var grip = new Sphere(.075, 14);
            grip.setTranslateX(side * 1.35);
            grip.setTranslateY(-.06);
            grip.setTranslateZ(3.38);
            grip.setMaterial(ModelLibrary.lit("#FF8A3D", "#A03B0B"));
            console.getChildren().addAll(base, stick, grip);
        }

        // Miniature holographic destination planet projector above the right console deck.
        var emitter = new Cylinder(.26, .08, 24);
        emitter.setTranslateX(1.68);
        emitter.setTranslateY(.30);
        emitter.setTranslateZ(3.25);
        emitter.setMaterial(ModelLibrary.lit("#1E6478", "#104A5E"));

        holoPlanet.setTranslateX(1.68);
        holoPlanet.setTranslateY(-.08);
        holoPlanet.setTranslateZ(3.25);
        holoPlanet.setMaterial(ModelLibrary.lit("#3CD9F5", "#137D96"));

        var ring = Meshes.view(Meshes.torus(.32, .018, 36, 8), ModelLibrary.lit("#8BF7FF", "#28C7DB"));
        ring.setTranslateX(1.68);
        ring.setTranslateY(-.08);
        ring.setTranslateZ(3.25);
        ring.setRotationAxis(Rotate.X_AXIS);
        ring.setRotate(24);

        console.getChildren().addAll(emitter, holoPlanet, ring);
        room.getChildren().add(console);
        return ring;
    }

    private Sphere buildGravityUnit(Group room) {
        // Capsule Corp Gravity Training Machine column on the left side wall.
        var unit = new Group();
        var column = new Cylinder(.55, 3.6, 28);
        column.setTranslateX(-3.25);
        column.setTranslateY(.15);
        column.setTranslateZ(-1.0);
        column.setMaterial(ModelLibrary.surface("#C8D4E3"));

        var core = new Cylinder(.38, 1.9, 24);
        core.setTranslateX(-3.25);
        core.setTranslateY(.10);
        core.setTranslateZ(-1.0);
        core.setMaterial(ModelLibrary.lit("#26E3F2", "#0E8596"));

        var ring = new Sphere(.62, 24);
        ring.setScaleY(.18);
        ring.setTranslateX(-3.25);
        ring.setTranslateY(-.45);
        ring.setTranslateZ(-1.0);
        ring.setMaterial(ModelLibrary.lit("#FF9A3C", "#B04E0C"));

        var cap = new Sphere(.58, 24);
        cap.setScaleY(.45);
        cap.setTranslateX(-3.25);
        cap.setTranslateY(-1.68);
        cap.setTranslateZ(-1.0);
        cap.setMaterial(ModelLibrary.surface("#3B4D66"));

        unit.getChildren().addAll(column, core, ring, cap);
        room.getChildren().add(unit);
        return ring;
    }

    private void buildPilotAndSeat(Group room) {
        // Ergonomic captain's seat at z = 1.05.
        var seat = new Group();
        var pedestal = new Cylinder(.34, .65, 20);
        pedestal.setTranslateY(1.62);
        pedestal.setTranslateZ(1.05);
        pedestal.setMaterial(ModelLibrary.surface("#203047"));

        var cushion = box(1.32, .28, 1.18, 0, 1.24, 1.08, "#1A2E4B");
        var trim = box(1.36, .08, 1.22, 0, 1.32, 1.08, "#F39C38");
        var backrest = box(1.26, 1.56, .26, 0, .48, .50, "#243D5E");
        backrest.setRotationAxis(Rotate.X_AXIS);
        backrest.setRotate(-6);
        var headrest = box(.76, .42, .26, 0, -.42, .45, "#F39C38");
        seat.getChildren().addAll(pedestal, cushion, trim, backrest, headrest);

        for (int side : new int[]{-1, 1}) {
            seat.getChildren().add(box(.18, .22, .95, side * .68, .78, 1.05, "#2C4669"));
            seat.getChildren().add(lamp(.12, .05, .28, side * .68, .66, 1.28, "#24E0F0"));
        }

        // Rider (friendly astronaut in Goku-inspired orange/blue space flight suit).
        pilotTorso.getChildren().addAll(
                box(.94, .92, .48, 0, .54, 1.04, "#F2F3EE"),
                box(.56, .76, .51, 0, .52, 1.04, "#F39C38"),
                box(.96, .18, .52, 0, .82, 1.04, "#1C62B5"),
                // Thighs & boots reaching toward the console pedals.
                box(.29, .27, .88, -.26, 1.06, 1.55, "#F39C38"),
                box(.29, .27, .88, .26, 1.06, 1.55, "#F39C38"),
                box(.26, .46, .28, -.26, 1.40, 1.92, "#1C62B5"),
                box(.26, .46, .28, .26, 1.40, 1.92, "#1C62B5"),
                // Arms resting forward toward armrests/joysticks.
                box(.24, .24, .75, -.58, .58, 1.32, "#F2F3EE"),
                box(.24, .24, .75, .58, .58, 1.32, "#F2F3EE"),
                box(.22, .20, .24, -.58, .60, 1.72, "#1C62B5"),
                box(.22, .20, .24, .58, .60, 1.72, "#1C62B5"));

        var helmet = new Sphere(.44, 28);
        helmet.setTranslateY(-.26);
        helmet.setTranslateZ(1.10);
        helmet.setMaterial(ModelLibrary.surface("#F7F6F0"));

        var visor = new Sphere(.37, 28);
        visor.setTranslateY(-.26);
        visor.setTranslateZ(1.29);
        visor.setScaleZ(.58);
        visor.setMaterial(SpaceShips.shiny("#1D5687"));

        var visorGlow = new Sphere(.30, 20);
        visorGlow.setTranslateY(-.30);
        visorGlow.setTranslateZ(1.36);
        visorGlow.setScaleZ(.42);
        visorGlow.setMaterial(ModelLibrary.lit("#2C7FB8", "#0E3C66"));

        pilotHead.getChildren().addAll(helmet, visor, visorGlow);
        for (int side : new int[]{-1, 1}) {
            var earPod = new Cylinder(.12, .10, 16);
            earPod.setRotationAxis(Rotate.Z_AXIS);
            earPod.setRotate(90);
            earPod.setTranslateX(side * .43);
            earPod.setTranslateY(-.26);
            earPod.setTranslateZ(1.10);
            earPod.setMaterial(ModelLibrary.lit("#F39C38", "#8A4608"));
            pilotHead.getChildren().add(earPod);
        }

        pilot.getChildren().addAll(seat, pilotTorso, pilotHead);
        room.getChildren().add(pilot);
    }

    public void station(int station) {
        switch (station) {
            case 1 -> { // Pilot seat (clear 3/4 view of rider, seat, and central console)
                camera.setTranslateX(1.75);
                camera.setTranslateY(-.34);
                camera.setTranslateZ(.25);
                cameraYaw.setAngle(-34);
                cameraPitch.setAngle(-4);
                pilot.setVisible(true);
            }
            case 2 -> { // Flight console
                camera.setTranslateX(0);
                camera.setTranslateY(-.22);
                camera.setTranslateZ(1.92);
                cameraYaw.setAngle(0);
                cameraPitch.setAngle(-4);
                pilot.setVisible(false);
            }
            case 3 -> { // Observation dome
                camera.setTranslateX(0);
                camera.setTranslateY(-1.10);
                camera.setTranslateZ(4.15);
                cameraYaw.setAngle(0);
                cameraPitch.setAngle(2);
                pilot.setVisible(false);
            }
            default -> { // Cabin aisle (0)
                camera.setTranslateX(0);
                camera.setTranslateY(-.18);
                camera.setTranslateZ(-4.05);
                cameraYaw.setAngle(0);
                cameraPitch.setAngle(0);
                pilot.setVisible(true);
            }
        }
    }

    public void setLightMode(int mode) {
        lightMode = ((mode % 3) + 3) % 3;
        switch (lightMode) {
            case 1 -> { // Night Cruise
                mainLight.setColor(Color.web("#5C9BE6"));
                ambientLight.setColor(Color.web("#394866"));
                consoleGlow.setColor(Color.web("#2CE8F5"));
                recolorLamps("#3AA8FF");
            }
            case 2 -> { // Warp Neon
                mainLight.setColor(Color.web("#AF7AE6"));
                ambientLight.setColor(Color.web("#473C6B"));
                consoleGlow.setColor(Color.web("#22F5FF"));
                recolorLamps("#22E3F2");
            }
            default -> { // Warm Day
                mainLight.setColor(Color.web("#BACCE0"));
                ambientLight.setColor(Color.web("#586785"));
                consoleGlow.setColor(Color.web("#32D6EB"));
                recolorLamps("#E69E45");
            }
        }
    }

    public void triggerRadarPing() {
        radarPingAlpha = 1.0;
    }

    public void update(double progress) {
        double p = Math.clamp(progress, 0, 1);
        radarSweep = (radarSweep + 0.14) % Math.TAU;
        radarPingAlpha = Math.max(0, radarPingAlpha - 0.08);

        // Gentle breathing & head turn for the rider.
        double breath = Math.sin(p * 42) * .022;
        pilotTorso.setTranslateY(breath);
        pilotHead.setTranslateY(breath * 1.2);
        pilotHead.setRotationAxis(Rotate.Y_AXIS);
        pilotHead.setRotate(Math.sin(p * 19) * 5.0);

        // Holographic globe & gravity unit spin.
        holoPlanet.setRotationAxis(Rotate.Y_AXIS);
        holoPlanet.setRotate(p * 900);
        holoRing.setRotate(24 + Math.sin(p * 30) * 8);
        gravityRing.setTranslateY(-.45 + Math.sin(p * 28) * .35);

        // Console button blinkers.
        for (int i = 0; i < consoleButtons.size(); i++) {
            consoleButtons.get(i).setOpacity(.55 + .45 * Math.pow(Math.sin(p * 55 + i * .9), 2));
        }

        if (inFlight) {
            for (int i = 0; i < stars.length; i++) {
                if (stars[i] != null) {
                    stars[i].setOpacity(.45 + .55 * Math.pow(Math.sin(p * 40 + i * 1.6), 2));
                }
            }
            for (int i = 0; i < warpStreaks.length; i++) {
                var s = warpStreaks[i];
                if (s != null) {
                    double z = 16.5 - ((p * 48 + i * 0.85) % 9.0);
                    s.setTranslateZ(z);
                }
            }
            if (outsidePod != null) {
                outsidePod.setTranslateX(-2.3 + Math.sin(p * 18) * 1.4);
                outsidePod.setTranslateY(-1.6 + Math.cos(p * 14) * .5);
                outsidePod.setRotate(p * 720);
            }
            outsidePlanet.setRotationAxis(Rotate.Y_AXIS);
            outsidePlanet.setRotate(p * 180);
        }
    }

    /** Updates the live graphical display on the 3D central console screen. */
    public void updateConsole(String route, String phase, String eta,
                              double fuelUsedKg, double fuelLeftKg, double plannedFuelKg,
                              double powerDrawW, double powerSupplyW,
                              double massKg, double budgetMusd,
                              String gravityLabel, String telemetryNote, double progress) {
        String signature = route + "|" + phase + "|" + eta + "|"
                + Math.round(fuelUsedKg) + "|" + Math.round(fuelLeftKg) + "|"
                + Math.round(powerDrawW) + "|" + Math.round(powerSupplyW) + "|"
                + gravityLabel + "|" + telemetryNote + "|" + Math.round(radarPingAlpha * 10);
        if (signature.equals(lastSignature)) {
            return;
        }
        lastSignature = signature;

        // Color-code the hologram and outside planet to match the mission destination.
        String lowerRoute = route == null ? "" : route.toLowerCase(Locale.ROOT);
        if (lowerRoute.contains("mars")) {
            holoPlanet.setMaterial(ModelLibrary.lit("#FF8A4C", "#A83B12"));
            outsidePlanet.setMaterial(ModelLibrary.lit("#D96B43", "#732C16"));
        } else if (lowerRoute.contains("moon")) {
            holoPlanet.setMaterial(ModelLibrary.lit("#CFD8E6", "#66748A"));
            outsidePlanet.setMaterial(ModelLibrary.lit("#B9C3D1", "#546070"));
        } else {
            holoPlanet.setMaterial(ModelLibrary.lit("#3CD9F5", "#137D96"));
            outsidePlanet.setMaterial(ModelLibrary.lit("#4F92D6", "#163B6B"));
        }

        drawConsoleCanvas(route, phase, eta, fuelUsedKg, fuelLeftKg, plannedFuelKg,
                powerDrawW, powerSupplyW, massKg, budgetMusd, gravityLabel, telemetryNote);
        if (Platform.isFxApplicationThread()) {
            screenCanvas.snapshot(null, screenImage);
        }
    }

    private void drawConsoleCanvas(String route, String phase, String eta,
                                   double fuelUsedKg, double fuelLeftKg, double plannedFuelKg,
                                   double powerDrawW, double powerSupplyW,
                                   double massKg, double budgetMusd,
                                   String gravityLabel, String telemetryNote) {
        GraphicsContext g = screenCanvas.getGraphicsContext2D();
        g.setFill(Color.web("#08192C"));
        g.fillRect(0, 0, SCREEN_W, SCREEN_H);

        // Subtle tactical grid.
        g.setStroke(Color.web("#123959"));
        g.setLineWidth(1);
        for (int x = 0; x < SCREEN_W; x += 32) {
            g.strokeLine(x, 0, x, SCREEN_H);
        }
        for (int y = 0; y < SCREEN_H; y += 32) {
            g.strokeLine(0, y, SCREEN_W, y);
        }

        // Top header bar.
        g.setFill(Color.web("#0D2C4A"));
        g.fillRect(0, 0, SCREEN_W, 40);
        g.setStroke(Color.web("#24E0F0"));
        g.setLineWidth(2);
        g.strokeLine(0, 40, SCREEN_W, 40);

        g.setFill(Color.web("#24E0F0"));
        g.setFont(Font.font("System", FontWeight.BOLD, 16));
        g.fillText("CAPSULE SHIP · CENTRAL FLIGHT CONSOLE", 14, 26);

        g.setFill(Color.web("#5DF29A"));
        g.fillOval(SCREEN_W - 145, 13, 12, 12);
        g.setFont(Font.font("System", FontWeight.BOLD, 14));
        g.fillText("SYSTEMS GO", SCREEN_W - 126, 25);

        // Left card: Destination & Time Left.
        drawPanel(g, 14, 52, 238, 126, "#24E0F0");
        g.setFill(Color.web("#8EEEF8"));
        g.setFont(Font.font("System", FontWeight.BOLD, 12));
        g.fillText("DESTINATION & ROUTE", 24, 72);
        g.setFill(Color.WHITE);
        g.setFont(Font.font("System", FontWeight.BOLD, 16));
        g.fillText(truncate(route, 24), 24, 94);

        g.setFill(Color.web("#FFB85C"));
        g.setFont(Font.font("System", FontWeight.BOLD, 12));
        g.fillText("CURRENT STATUS", 24, 118);
        g.setFill(Color.web("#F3F8FF"));
        g.setFont(Font.font("System", FontWeight.BOLD, 14));
        g.fillText(truncate(phase, 26), 24, 136);

        g.setFill(Color.web("#5DF29A"));
        g.setFont(Font.font("System", FontWeight.BOLD, 15));
        g.fillText("ETA: " + truncate(eta, 22), 24, 164);

        // Bottom-left card: Fuel Consumption.
        drawPanel(g, 14, 188, 238, 98, "#FF9A3C");
        g.setFill(Color.web("#FFCB69"));
        g.setFont(Font.font("System", FontWeight.BOLD, 12));
        g.fillText("FUEL CONSUMPTION", 24, 208);

        double fuelRatio = plannedFuelKg > 0 ? Math.clamp(fuelLeftKg / plannedFuelKg, 0, 1) : 1.0;
        g.setFill(Color.web("#0A1626"));
        g.fillRoundRect(24, 218, 216, 20, 8, 8);
        g.setFill(fuelRatio > .25 ? Color.web("#3CE89E") : Color.web("#FF725C"));
        g.fillRoundRect(24, 218, Math.max(8, 216 * fuelRatio), 20, 8, 8);
        g.setStroke(Color.web("#8CC4E8"));
        g.strokeRoundRect(24, 218, 216, 20, 8, 8);

        g.setFill(Color.WHITE);
        g.setFont(Font.font("System", FontWeight.BOLD, 12));
        String fuelSummary = plannedFuelKg <= 0
                ? "Orbital Cruise (No Main Burn)"
                : String.format("Used: %.0f kg  |  Left: %.0f kg", fuelUsedKg, Math.max(0, fuelLeftKg));
        g.fillText(fuelSummary, 24, 260);
        g.setFill(Color.web("#B9C7E6"));
        g.fillText(String.format("Tank Level: %.0f%%", fuelRatio * 100), 24, 278);

        // Center-right card: Ship Stats & Power.
        drawPanel(g, 264, 52, 224, 234, "#4BA8FF");
        g.setFill(Color.web("#80EBF5"));
        g.setFont(Font.font("System", FontWeight.BOLD, 13));
        g.fillText("SHIP TELEMETRY & STATS", 276, 74);

        g.setFill(Color.WHITE);
        g.setFont(Font.font("System", FontWeight.BOLD, 14));
        g.fillText(String.format("Power: %.0f / %.0f W", powerDrawW, powerSupplyW), 276, 102);
        double pwrRatio = powerSupplyW > 0 ? Math.clamp(powerDrawW / powerSupplyW, 0, 1) : 0;
        g.setFill(Color.web("#0A1626"));
        g.fillRect(276, 110, 196, 10);
        g.setFill(pwrRatio > .95 ? Color.web("#FF8A4C") : Color.web("#24E0F0"));
        g.fillRect(276, 110, Math.max(6, 196 * pwrRatio), 10);

        g.setFill(Color.WHITE);
        g.fillText(String.format("Mass: %.0f kg", massKg), 276, 144);
        g.fillText(String.format("Budget: $%.1fM", budgetMusd), 276, 168);
        g.setFill(Color.web("#FFCB69"));
        g.fillText(truncate(gravityLabel, 24), 276, 196);

        g.setFill(Color.web("#9FE8FF"));
        g.setFont(Font.font("System", FontWeight.NORMAL, 12));
        drawWrapped(g, telemetryNote, 276, 224, 198);

        // Right radar circle.
        drawPanel(g, 500, 52, 126, 234, "#5DF29A");
        double rcx = 563;
        double rcy = 138;
        double rr = 48;
        g.setStroke(Color.web("#2B8F6A"));
        g.setLineWidth(1.5);
        g.strokeOval(rcx - rr, rcy - rr, rr * 2, rr * 2);
        g.strokeOval(rcx - rr * .55, rcy - rr * .55, rr * 1.1, rr * 1.1);
        g.strokeLine(rcx - rr, rcy, rcx + rr, rcy);
        g.strokeLine(rcx, rcy - rr, rcx, rcy + rr);

        // Sweeping radar beam.
        g.setStroke(Color.web("#5DF29A"));
        g.setLineWidth(2.4);
        g.strokeLine(rcx, rcy, rcx + Math.cos(radarSweep) * rr, rcy + Math.sin(radarSweep) * rr);

        if (radarPingAlpha > 0) {
            g.setStroke(Color.color(.4, 1, .7, radarPingAlpha));
            g.strokeOval(rcx - rr * (1.1 - radarPingAlpha * .4), rcy - rr * (1.1 - radarPingAlpha * .4),
                    rr * 2 * (1.1 - radarPingAlpha * .4), rr * 2 * (1.1 - radarPingAlpha * .4));
        }

        // Blips for nearby NASA craft & space pods.
        g.setFill(Color.web("#FFD273"));
        g.fillOval(rcx + 18, rcy - 22, 6, 6);
        g.setFill(Color.web("#24E0F0"));
        g.fillOval(rcx - 24, rcy + 14, 6, 6);

        g.setFill(Color.web("#5DF29A"));
        g.setFont(Font.font("System", FontWeight.BOLD, 12));
        g.fillText("SECTOR RADAR", 516, 216);
        g.setFill(Color.web("#B9C7E6"));
        g.setFont(Font.font("System", FontWeight.NORMAL, 11));
        g.fillText("Pods & Satellites", 515, 236);
        g.fillText("Tracked Live", 528, 252);
    }

    private static void drawPanel(GraphicsContext g, double x, double y, double w, double h, String borderHex) {
        g.setFill(Color.web("#0B2038", .88));
        g.fillRoundRect(x, y, w, h, 10, 10);
        g.setStroke(Color.web(borderHex, .8));
        g.setLineWidth(1.5);
        g.strokeRoundRect(x, y, w, h, 10, 10);
    }

    private static void drawWrapped(GraphicsContext g, String text, double x, double y, double maxWidth) {
        if (text == null || text.isBlank()) {
            return;
        }
        String clean = text.replace('\n', ' ');
        int split = Math.min(clean.length(), 28);
        if (clean.length() <= 28) {
            g.fillText(clean, x, y);
            return;
        }
        int space = clean.lastIndexOf(' ', split);
        int cut = space > 10 ? space : split;
        g.fillText(clean.substring(0, cut).trim(), x, y);
        g.fillText(truncate(clean.substring(cut).trim(), 28), x, y + 18);
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    private void recolorLamps(String hex) {
        var material = ModelLibrary.lit(hex, hex);
        for (var lamp : ceilingLamps) {
            lamp.setMaterial(material);
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
