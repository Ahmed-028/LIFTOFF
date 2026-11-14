package com.liftoff.render3d;

import com.liftoff.render.RocketArt;
import com.liftoff.render.Theme;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SnapshotParameters;
import javafx.scene.SubScene;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/**
 * The opening cutscene: Nova, a round young explorer in an astronaut cap, walks into a bedroom,
 * opens a laptop and loads LIFTOFF, and the camera flies into the screen. Everything is placed
 * from the time alone ({@link #update(double)}), so the scene can be skipped or paused anywhere.
 *
 * <p>Units are roughly metres; y points down, the floor is at y = 0 and the back wall at z = 5.
 */
public final class IntroScene extends CutsceneView {

    /** Length of the cutscene in seconds. */
    public static final double LENGTH = 12.2;

    private static final int SCREEN_FRAMES = 14;
    private static final double[][] WALK = {{-4.1, 6.2}, {-4.1, 4.1}, {-2.2, 2.4}, {0.4, 2.0}, {1.8, 2.1}};
    private static final double DESK_X = 1.8;

    private final RoundFigure kid = new RoundFigure(RoundFigure.Look.KID_NOVA);
    private final Group door = new Group();
    /** Turns the door about its left edge (setRotate would turn it about its middle). */
    private final Rotate hinge = new Rotate(0, 0, 0, 0, Rotate.Y_AXIS);
    private final Group lid = new Group();
    private final PhongMaterial screenMaterial = new PhongMaterial(Color.web("#071330"));
    private final Image[] screenFrames = new Image[SCREEN_FRAMES];
    private final PointLight screenGlow = new PointLight(Color.BLACK);

    public IntroScene() {
        super(Theme.NAVY_950, Color.web("#3C4870"));
        var room = new Group();
        buildRoom(room);
        buildDesk(room);
        room.getChildren().add(kid);

        var lamp = new PointLight(Color.web("#FFE6BE"));
        Meshes.at(lamp, -1, -4.6, 0);
        var fill = new PointLight(Color.web("#4A5E9E"));
        Meshes.at(fill, 4, -3, -6);
        Meshes.at(screenGlow, DESK_X, -1.9, 2.8);
        world.getChildren().addAll(room, lamp, fill, screenGlow);

        for (int i = 0; i < SCREEN_FRAMES; i++) {
            screenFrames[i] = loadingScreen(i / (double) (SCREEN_FRAMES - 1));
        }
        update(0);
    }

    @Override
    public double length() {
        return LENGTH;
    }

    /** Places everything for the given second of the cutscene. */
    @Override
    public void update(double t) {
        // Door: swings open, stays open.
        // The door swings fully open, out into the hall and out of Nova's way, before Nova walks in.
        hinge.setAngle(-95 * ease(clamp((t - .4) / .9)));

        // Walk in along the path, then stop at the desk and turn to it.
        double walk = clamp((t - 1.6) / 3.5);
        double[] at = along(walk);
        double[] ahead = along(Math.min(1, walk + .02));
        kid.setTranslateX(at[0]);
        kid.setTranslateZ(at[1]);
        double heading = Math.toDegrees(Math.atan2(-(ahead[0] - at[0]), -(ahead[1] - at[1])));
        boolean walking = walk > 0 && walk < 1;
        // At the desk Nova turns to wave hello, then turns to the laptop.
        double hello = ease(clamp((t - 5.1) / .4));
        double toDesk = ease(clamp((t - 6.2) / .5));
        double facing = walk <= 0 ? 0 : t < 5.1 ? heading : lerpAngle(lerpAngle(heading, 0, hello), 180, toDesk);
        kid.setRotationAxis(Rotate.Y_AXIS);
        kid.setRotate(facing);
        kid.bob(t, walking);
        // Hands reach for the keyboard once the laptop is open.
        double reach = ease(clamp((t - 7.6) / .6));
        if (reach > 0) {
            kid.hands(0, reach);
            if (t > 8.2) {
                kid.type(t);
            }
        }
        if (t > 5.4 && t < 6.2) {
            // Waving hello to the player.
            kid.wave(t - 5.4);
        }

        // Laptop lid opens; the screen lights up and loads.
        lid.setRotate(90 - 105 * ease(clamp((t - 6.8) / 1.0)));
        double loading = clamp((t - 7.9) / 2.2);
        boolean on = t > 7.3;
        screenMaterial.setSelfIlluminationMap(on ? screenFrames[(int) Math.round(loading * (SCREEN_FRAMES - 1))] : null);
        screenMaterial.setDiffuseColor(on ? Color.web("#1D3A70") : Color.web("#071330"));
        double glow = on ? clamp((t - 7.3) / .6) : 0;
        screenGlow.setColor(Color.web("#22E3F2").deriveColor(0, 1, glow * .8, 1));

        // Camera: wide shot, over the shoulder, then into the screen.
        double shoulder = ease(clamp((t - 6.2) / 1.3));
        double dive = ease(clamp((t - 10.2) / 1.7));
        double[] wide = {0, -3.1, -7.2, 0, -1.4, 3};
        double[] over = {-.7, -3.55, -.5, DESK_X, -1.62, 3.5};
        double[] into = {DESK_X, -1.68, 2.98, DESK_X, -1.68, 4.5};
        double[] shot = new double[6];
        for (int i = 0; i < 6; i++) {
            shot[i] = lerp(lerp(wide[i], over[i], shoulder), into[i], dive);
        }
        look(shot[0], shot[1], shot[2], shot[3], shot[4], shot[5]);
    }

    private static double[] along(double progress) {
        double scaled = progress * (WALK.length - 1);
        int leg = Math.min(WALK.length - 2, (int) Math.floor(scaled));
        double f = scaled - leg;
        return new double[] {lerp(WALK[leg][0], WALK[leg + 1][0], f), lerp(WALK[leg][1], WALK[leg + 1][1], f)};
    }

    // ---- building ----

    private void buildRoom(Group room) {
        room.getChildren().addAll(
                box(13, .2, 13, 0, .1, .5, "#6A4630"),
                // The back wall has a doorway on the left.
                box(1.4, 6.4, .2, -5.8, -3.2, 5.1, "#27427E"),
                box(9.6, 6.4, .2, 1.7, -3.2, 5.1, "#27427E"),
                box(2.0, 3.4, .2, -4.1, -4.7, 5.1, "#27427E"),
                box(.2, 6.4, 11, -6.5, -3.2, 0, "#284380"),
                box(.2, 6.4, 11, 6.5, -3.2, 0, "#284380"),
                box(13, .2, 11, 0, -6.4, 0, "#1E3263"));
        // Skirting board in the theme orange.
        room.getChildren().add(box(13, .25, .22, 0, -.12, 4.98, "#FF7A1A"));
        // Round rug.
        var rug = new Cylinder(2.3, .04, 48);
        rug.setMaterial(ModelLibrary.surface("#22A6B5"));
        Meshes.at(rug, -.5, -.02, 1.6);
        var rugInner = new Cylinder(1.5, .05, 48);
        rugInner.setMaterial(ModelLibrary.surface("#FFCB69"));
        Meshes.at(rugInner, -.5, -.03, 1.6);
        room.getChildren().addAll(rug, rugInner);
        // Little stars on the wallpaper.
        var random = new java.util.SplittableRandom(4);
        for (int i = 0; i < 46; i++) {
            double x = random.nextDouble(-6.2, 6.2);
            double y = random.nextDouble(-6, -2.8);
            if (Math.abs(x - 2.6) < 1.7 && y > -5.6) {
                continue;
            }
            var star = new Sphere(random.nextDouble(.03, .07), 8);
            star.setMaterial(ModelLibrary.lit("#FFE29A", "#8A6A1A"));
            Meshes.at(star, x, y, 4.98);
            room.getChildren().add(star);
        }
        // Night window with a moon.
        room.getChildren().add(box(2.6, 1.9, .1, 2.6, -4.0, 5.0, "#0B1A44"));
        room.getChildren().add(frame(2.6, 1.9, 2.6, -4.0, 4.95));
        var moon = new Sphere(.32, 24);
        moon.setMaterial(ModelLibrary.lit("#E9EDF7", "#7C8396"));
        Meshes.at(moon, 3.3, -4.45, 4.93);
        room.getChildren().add(moon);
        for (int i = 0; i < 9; i++) {
            var star = new Sphere(.025, 6);
            star.setMaterial(ModelLibrary.lit("#FFFFFF", "#FFFFFF"));
            Meshes.at(star, 1.6 + random.nextDouble(1.9), -3.3 - random.nextDouble(1.3), 4.94);
            room.getChildren().add(star);
        }
        // Rocket poster.
        var poster = new MeshView(Meshes.quad(1.3, 1.8));
        var posterMaterial = new PhongMaterial(Color.WHITE);
        posterMaterial.setDiffuseMap(poster());
        posterMaterial.setSelfIlluminationMap(poster());
        poster.setMaterial(posterMaterial);
        poster.setCullFace(CullFace.NONE);
        Meshes.at(poster, -1.6, -3.7, 4.98);
        room.getChildren().add(poster);
        // Bed in the right corner.
        room.getChildren().addAll(
                box(2.4, .5, 3.6, 5.1, -.35, 2.6, "#C07A47"),
                box(2.3, .32, 3.5, 5.1, -.75, 2.6, "#F2F5FA"),
                box(2.34, .2, 2.4, 5.1, -.95, 2.0, "#FF7A1A"),
                box(1.4, .28, .7, 5.1, -1.02, 4.0, "#DCE8F7"));
        // The door, hinged on its left edge, with warm hall light behind it.
        room.getChildren().add(box(3.2, 3.4, .05, -4.1, -1.7, 7.4, "#FFD08A"));
        var hallLight = new PointLight(Color.web("#FFC37A"));
        Meshes.at(hallLight, -4.1, -2.4, 6.6);
        room.getChildren().add(hallLight);
        var panel = box(1.96, 2.9, .12, .98, -1.45, .08, "#B9774A");
        var knob = sphere(.07, "#D9AE5E");
        Meshes.at(knob, 1.75, -1.4, -.1);
        door.getChildren().addAll(panel, knob);
        door.setTranslateX(-5.08);
        door.setTranslateZ(4.96);
        door.getTransforms().add(hinge);
        room.getChildren().add(door);
        room.getChildren().add(frame(2.02, 3.02, -4.1, -1.51, 5.0));
    }

    private void buildDesk(Group room) {
        room.getChildren().addAll(
                box(2.8, .12, 1.4, DESK_X, -1.24, 3.5, "#E8EEF6"),
                box(.12, 1.2, .12, DESK_X - 1.25, -.6, 2.95, "#16305E"),
                box(.12, 1.2, .12, DESK_X + 1.25, -.6, 2.95, "#16305E"),
                box(.12, 1.2, .12, DESK_X - 1.25, -.6, 4.05, "#16305E"),
                box(.12, 1.2, .12, DESK_X + 1.25, -.6, 4.05, "#16305E"));
        // Desk lamp and a little globe.
        var lampBase = new Cylinder(.18, .06, 24);
        lampBase.setMaterial(ModelLibrary.surface("#FF7A1A"));
        Meshes.at(lampBase, DESK_X + 1.0, -1.33, 3.9);
        var lampBulb = new Sphere(.16, 20);
        lampBulb.setMaterial(ModelLibrary.lit("#FFE7B0", "#FFC46A"));
        Meshes.at(lampBulb, DESK_X + 1.0, -1.9, 3.9);
        var lampStem = new Cylinder(.03, .55, 10);
        lampStem.setMaterial(ModelLibrary.surface("#16305E"));
        Meshes.at(lampStem, DESK_X + 1.0, -1.6, 3.9);
        var globe = new Sphere(.2, 24);
        globe.setMaterial(ModelLibrary.surface("#3EA0FF"));
        Meshes.at(globe, DESK_X - 1.0, -1.52, 3.8);
        room.getChildren().addAll(lampBase, lampStem, lampBulb, globe);

        // Laptop: a base on the desk and a lid hinged at its back edge.
        room.getChildren().add(box(1.0, .05, .66, DESK_X, -1.325, 3.25, "#BAC6D4"));
        room.getChildren().add(box(.86, .01, .4, DESK_X, -1.355, 3.18, "#2A3040"));
        var shell = box(1.0, .68, .03, 0, -.34, 0, "#BAC6D4");
        var display = new MeshView(Meshes.quad(.92, .6));
        display.setMaterial(screenMaterial);
        display.setCullFace(CullFace.NONE);
        Meshes.at(display, 0, -.34, -.02);
        var logo = sphere(.03, "#FF7A1A");
        Meshes.at(logo, 0, -.34, .02);
        lid.getChildren().addAll(shell, display, logo);
        lid.setTranslateX(DESK_X);
        lid.setTranslateY(-1.35);
        lid.setTranslateZ(3.58);
        lid.setRotationAxis(Rotate.X_AXIS);
        room.getChildren().add(lid);
    }

    private static Node frame(double w, double h, double x, double y, double z) {
        var frame = new Group(
                box(w + .2, .14, .14, x, y - h / 2, z, "#F2F5FA"),
                box(w + .2, .14, .14, x, y + h / 2, z, "#F2F5FA"),
                box(.14, h, .14, x - w / 2, y, z, "#F2F5FA"),
                box(.14, h, .14, x + w / 2, y, z, "#F2F5FA"));
        return frame;
    }

    private static Image poster() {
        var art = RocketArt.create(250);
        var canvas = new Canvas(300, 420);
        var g = canvas.getGraphicsContext2D();
        g.setFill(Theme.NAVY_700);
        g.fillRect(0, 0, 300, 420);
        g.setFill(Theme.ORANGE);
        g.fillRect(0, 370, 300, 50);
        g.setFill(Theme.NAVY_950);
        g.setFont(Theme.display(40));
        g.setTextAlign(TextAlignment.CENTER);
        g.fillText("TO THE STARS", 150, 405);
        var params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        var rocket = art.snapshot(params, null);
        g.drawImage(rocket, 150 - rocket.getWidth() / 2, 40);
        return canvas.snapshot(null, null);
    }

    /** The laptop screen: the LIFTOFF logo and a loading bar at the given progress. */
    private static Image loadingScreen(double progress) {
        var canvas = new Canvas(460, 300);
        var g = canvas.getGraphicsContext2D();
        g.setFill(Theme.NAVY_900);
        g.fillRect(0, 0, 460, 300);
        var random = new java.util.SplittableRandom(3);
        g.setFill(Theme.alpha(Theme.TEXT, .7));
        for (int i = 0; i < 40; i++) {
            g.fillOval(random.nextDouble(460), random.nextDouble(300), 2, 2);
        }
        g.setTextAlign(TextAlignment.CENTER);
        g.setFill(Theme.TEXT);
        g.setFont(Theme.display(84));
        g.fillText("LIFTOFF", 230, 150);
        g.setFill(Theme.CYAN);
        g.setFont(Theme.bold(20));
        g.fillText(progress < 1 ? "Loading your mission…" : "Ready!", 230, 196);
        g.setFill(Theme.NAVY_700);
        g.fillRoundRect(70, 222, 320, 22, 22, 22);
        g.setFill(Theme.ORANGE);
        g.fillRoundRect(70, 222, Math.max(22, 320 * progress), 22, 22, 22);
        return canvas.snapshot(null, null);
    }
}
