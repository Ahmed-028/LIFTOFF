package com.liftoff.render3d;

import com.liftoff.model.ModuleSlot;
import com.liftoff.session.stub.Mission.Destination;
import javafx.animation.AnimationTimer;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SplittableRandom;


public final class FlightView extends StackPane {
    public enum Phase { DEPARTURE, CRUISE, ORBIT, ENTRY, LANDING, SURFACE, RETURN }


    enum Shot { CHASE, FLYBY, SIDE, SPIN, ORBIT_CAM }

    static final double SHOT_SECONDS = 6.2;
    private static final List<Shot> DEPARTURE_SHOTS = List.of(Shot.SPIN, Shot.ORBIT_CAM, Shot.CHASE, Shot.SIDE);
    private static final List<Shot> CRUISE_SHOTS = List.of(Shot.CHASE, Shot.FLYBY, Shot.SIDE, Shot.SPIN, Shot.ORBIT_CAM);
    private static final List<Shot> RETURN_SHOTS = List.of(Shot.CHASE, Shot.SPIN, Shot.FLYBY);
    private static final double HERO_FACING = 180;

    private final Destination destination;
    private final ImageView terrain = new ImageView();
    private final SpaceBackdrop backdrop;
    private final Canvas overlay = new Canvas();
    private final CameraRig rig = new CameraRig();
    private final Group shipRig = new Group();
    private final Translate shipPosition = new Translate();
    private final Rotate shipYaw = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate shipPitch = new Rotate(0, Rotate.X_AXIS);
    private final Rotate shipRoll = new Rotate(0, Rotate.Z_AXIS);
    private final Group ship;
    private final Group payload;
    private final Sphere earth;
    private final Sphere target;
    private final Sphere shadow;
    private final Group exhaust = new Group();
    private final Group entryFire = new Group();
    private final Rotate payloadTurn = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate targetTurn = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate earthTurn = new Rotate(0, Rotate.Y_AXIS);
    private final Escort capsule;
    private final Escort[] pods = new Escort[2];
    private final List<MeshView> rocks = new ArrayList<>();
    private final double[] rockSpin;
    private final Group ringedPlanet;
    private final Group nasaSatellite;
    private final Rotate nasaTurn = new Rotate(0, Rotate.Y_AXIS);
    private final AnimationTimer timer;

    private double progress;
    private Phase phase = Phase.DEPARTURE;
    private double time;
    private long lastNanos;
    private double lastDt = 1 / 60.0;
    private boolean calm;
    private double warpFlashAt = -10;
    private Shot shot = Shot.CHASE;

    public FlightView(Destination destination, Map<ModuleSlot, String> parts) {
        this.destination = destination;
        setMinSize(0, 220);
        getStyleClass().add("flight-viewport");
        var clip = new javafx.scene.shape.Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);
        setAlignment(Pos.CENTER);

        backdrop = new SpaceBackdrop(destination.name().toLowerCase(Locale.ROOT));
        backdrop.widthProperty().bind(widthProperty());
        backdrop.heightProperty().bind(heightProperty());
        backdrop.setManaged(false);
        overlay.widthProperty().bind(widthProperty());
        overlay.heightProperty().bind(heightProperty());
        overlay.setManaged(false);
        overlay.setMouseTransparent(true);
        if (destination != Destination.EARTH) {
            terrain.setImage(image(destination.name().toLowerCase(Locale.ROOT) + "-terrain.jpg"));
        }
        terrain.fitWidthProperty().bind(widthProperty());
        terrain.fitHeightProperty().bind(heightProperty());
        terrain.setManaged(false);

        var library = new ModelLibrary();
        var assembly = new ShipAssembly(library);
        parts.forEach(assembly::showPart);
        ship = framed(assembly.node(), 4.5);
        for (int i = 0; i < 8; i++) {
            var glow = new Sphere(.42 - i * .043, 16);
            glow.setTranslateZ(2.15 + i * .24);
            glow.setScaleZ(1.65);
            glow.setOpacity(.95 - i * .11);
            glow.setMaterial(ModelLibrary.lit(i < 2 ? "#B8FFFF" : "#3198FF", "#1A54A8"));
            exhaust.getChildren().add(glow);
        }
        ship.getChildren().add(exhaust);
        ship.getTransforms().add(new Rotate(HERO_FACING, Rotate.Y_AXIS));
        buildEntryFire();
        shipRig.getChildren().addAll(ship, entryFire);
        shipRig.getTransforms().addAll(shipPosition, shipYaw, shipPitch, shipRoll);

        var model = parts.get(ModuleSlot.SATELLITE);
        payload = framed(library.create(model == null ? "cubesat-3u" : model), 4.6);
        payload.getTransforms().add(payloadTurn);

        earth = globe("earth", "#427DAD");
        earth.getTransforms().add(earthTurn);
        target = globe(destination.name().toLowerCase(Locale.ROOT), destination == Destination.MARS ? "#BF683E" : "#9099A7");
        target.getTransforms().add(targetTurn);
        shadow = new Sphere(1.2, 24);
        shadow.setMaterial(ModelLibrary.surface("#201C23"));
        shadow.setScaleY(.045);
        shadow.setOpacity(.4);

        capsule = new Escort(SpaceShips.capsuleShip("#F4F1E8", "#FF8A3D"), 1.15);
        pods[0] = new Escort(SpaceShips.pod("#F2F4F7", "#E0446A"), .9);
        pods[1] = new Escort(SpaceShips.pod("#DDE8F5", "#8C5BFF"), .9);
        var random = new SplittableRandom(9);
        rockSpin = new double[7];
        for (int i = 0; i < rockSpin.length; i++) {
            var rock = SpaceShips.asteroid(i + 3, random.nextDouble(.6, 1.7));
            rock.setRotationAxis(new javafx.geometry.Point3D(random.nextDouble(-1, 1), 1, random.nextDouble(-1, 1)));
            rock.setUserData(new double[]{random.nextDouble(-28, -7) * (i % 2 == 0 ? 1 : -1) - (i % 2 == 0 ? 0 : 0),
                    random.nextDouble(-9, 9), random.nextDouble(-60, 60)});
            rockSpin[i] = random.nextDouble(20, 60);
            rocks.add(rock);
        }
        ringedPlanet = SpaceShips.ringedPlanet(24);
        nasaSatellite = framed(library.create("nasa-landsat7"), 3.2);
        nasaSatellite.getTransforms().add(nasaTurn);

        rig.camera.setNearClip(.1);
        rig.camera.setFarClip(2500);
        rig.camera.setFieldOfView(40);
        double[] sun = {-.52, -.42, -.74};
        backdrop.setSun(sun[0], sun[1], sun[2]);
        var light = new PointLight(Color.web("#FFF3DE"));
        light.setTranslateX(sun[0] * 1500);
        light.setTranslateY(sun[1] * 1500);
        light.setTranslateZ(sun[2] * 1500);
        var fill = new PointLight(Color.web("#4E64A8"));
        fill.setTranslateX(900);
        fill.setTranslateY(500);
        fill.setTranslateZ(700);
        var world = new Group(earth, target, ringedPlanet, nasaSatellite, shipRig, capsule.root, pods[0].root,
                pods[1].root, shadow, payload, rig.camera, light, fill, new AmbientLight(Color.web("#5C6782")));
        world.getChildren().addAll(rocks);
        var scene = new SubScene(world, 1, 1, true, SceneAntialiasing.BALANCED);
        scene.setMouseTransparent(true);
        scene.setFill(Color.TRANSPARENT);
        scene.setCamera(rig.camera);
        scene.widthProperty().bind(widthProperty());
        scene.heightProperty().bind(heightProperty());
        getChildren().addAll(backdrop, terrain, scene, overlay);

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double dt = lastNanos == 0 ? 1 / 60.0 : Math.min(.1, (now - lastNanos) / 1e9);
                lastNanos = now;
                time += dt;
                lastDt = dt;
                render();
            }
        };
        sceneProperty().addListener((property, before, value) -> {
            if (value == null) {
                timer.stop();
            } else {
                lastNanos = 0;
                timer.start();
            }
        });
        showProgress(0);
    }

    public static Phase phase(Destination destination, double progress) {
        if (destination == Destination.EARTH) {
            return progress < .18 ? Phase.DEPARTURE : Phase.ORBIT;
        }
        if (progress < .14) {
            return Phase.DEPARTURE;
        }
        if (progress < .42) {
            return Phase.CRUISE;
        }
        if (progress < .57) {
            return destination == Destination.MARS ? Phase.ENTRY : Phase.ORBIT;
        }
        if (progress < .69) {
            return Phase.LANDING;
        }
        if (destination == Destination.MOON && progress >= .90) {
            return Phase.RETURN;
        }
        return Phase.SURFACE;
    }


    public void setCalm(boolean calm) {
        this.calm = calm;
        render();
    }

    public void showProgress(double value) {
        progress = Math.clamp(value, 0, 1);
        Phase next = phase(destination, progress);
        if (next != phase && (next == Phase.CRUISE || next == Phase.RETURN)) {
            warpFlashAt = time;
        }
        phase = next;
        render();
    }


    void freezeAt(double seconds) {
        timer.stop();
        lastDt = 1 / 60.0;
        time = seconds;
        render();
    }

    Shot shot() {
        return shot;
    }

    private void render() {
        double p = progress;
        boolean surface = phase == Phase.LANDING || phase == Phase.SURFACE;
        boolean thrust = phase == Phase.DEPARTURE || phase == Phase.CRUISE || phase == Phase.RETURN;
        double burn = switch (phase) {
            case DEPARTURE -> ease(p / (destination == Destination.EARTH ? .18 : .14));
            case CRUISE -> ease((p - .14) / .28);
            case RETURN -> ease((p - .90) / .10);
            default -> 0;
        };
        terrain.setVisible(surface);
        backdrop.setVisible(!surface);
        shipRig.setVisible(!surface && phase != Phase.ORBIT);
        payload.setVisible(surface || phase == Phase.ORBIT);
        shadow.setVisible(surface);
        entryFire.setVisible(phase == Phase.ENTRY);
        exhaust.setVisible(thrust);
        ringedPlanet.setVisible(phase == Phase.CRUISE);
        nasaSatellite.setVisible(phase == Phase.DEPARTURE);
        for (var rock : rocks) {
            rock.setVisible(phase == Phase.CRUISE && !calm);
        }
        capsule.root.setVisible(thrust);
        pods[0].root.setVisible(thrust && !calm);
        pods[1].root.setVisible(thrust && !calm);

        double flow = 0;
        double streak = 0;
        double speedLines = 0;
        if (thrust) {
            double[] motion = flyThroughSpace(burn);
            flow = motion[0];
            streak = motion[1];
            speedLines = motion[2];
        } else if (phase == Phase.ORBIT) {
            showOrbit(p);
            flow = 0;
        } else if (phase == Phase.ENTRY) {
            showEntry(p);
            flow = calm ? 10 : 60;
            streak = calm ? 0 : .05;
        } else {
            showSurface(p);
        }

        double sinceWarp = time - warpFlashAt;
        double flash = calm ? 0 : Math.max(0, 1 - sinceWarp / .7);
        if (!calm && sinceWarp < 1.4) {
            speedLines = Math.max(speedLines, 1 - sinceWarp / 1.4);
        }
        if (!surface) {
            backdrop.draw(rig, flow, lastDt, streak, time, Color.TRANSPARENT, 0);
        }
        drawOverlay(speedLines, flash, surface);
    }


    private double[] flyThroughSpace(double burn) {
        List<Shot> shots = switch (phase) {
            case DEPARTURE -> DEPARTURE_SHOTS;
            case RETURN -> RETURN_SHOTS;
            default -> CRUISE_SHOTS;
        };
        double t = time;
        shot = calm ? Shot.CHASE : shots.get((int) Math.floor(t / SHOT_SECONDS) % shots.size());
        double u = calm ? .5 : (t % SHOT_SECONDS) / SHOT_SECONDS;
        double wobble = calm ? .25 : 1;
        double sx = .45 * Math.sin(t * 1.1) * wobble;
        double sy = .28 * Math.sin(t * 1.6 + 1) * wobble;
        double roll = 9 * Math.sin(t * .8) * wobble;
        double yaw = 4 * Math.sin(t * .6) * wobble;
        double pitch = 3 * Math.sin(t * .9) * wobble;
        double flow = phase == Phase.DEPARTURE ? 40 + 150 * burn : 190;
        double streak = .055;
        double lines = 0;
        double hx = sx, hy = sy, hz = 0;

        switch (shot) {
            case CHASE -> {
                rig.lookAt(hx * .45 + 1.4 * Math.sin(t * .31) * wobble, -2.4 + .35 * Math.sin(t * .5) * wobble, -13,
                        hx * .7, -.35, 6, Math.toRadians(-roll * .15));
                capsule.place(-5.6, .9 + .4 * Math.sin(t * .9), 3 + 1.2 * Math.sin(t * .4), t, 0);
                pods[0].place(3.8, -1.7, -42 + 120 * u, t, 1);
                pods[1].place(-2.6, 2.1, -75 + 120 * u, t, 1);
            }
            case FLYBY -> {
                hz = 22 * Math.tan((u - .5) * 2.8);
                hx = sx * .4;
                double pass = Math.exp(-Math.pow((u - .5) / .09, 2));
                roll += 70 * pass;
                rig.lookAt(5.2, -1.5, 0, hx, hy, hz, 0);
                capsule.place(-3.8, 1.6, hz - 10, t, 0);
                pods[0].place(1.8, -2.4, hz - 18, t, 1);
                pods[1].place(-1.5, 2.8, hz - 26, t, 1);
                flow = 55;
                streak = .03;
                lines = pass;
            }
            case SIDE -> {
                hx = sx * .4;
                rig.lookAt(11.5, -1.3 + .25 * Math.sin(t * .4), 1.8 - 4 * (u - .5), 0, -.25, 1.4, 0);
                capsule.place(-5, 1.2, -4.5 + Math.sin(t * .7), t, 0);
                pods[0].place(-2.5, -1.8, -30 + 70 * u, t, 1);
                pods[1].place(-9, 2.5, -55 + 90 * u, t, 1);
                streak = .07;
            }
            case SPIN -> {
                double barrel = phase == Phase.DEPARTURE && Math.floor(t / SHOT_SECONDS) == 0
                        ? 0 : 360 * ease((u - .22) / .5);
                roll += barrel;
                rig.lookAt(2.4 + .5 * Math.sin(t * .3), -1.4, 13.5, 0, -.15, 0, 0);
                capsule.place(4.5, 2.2, -19, t, 0);
                pods[0].place(-2.8, 1.3, -8.5, t, 1);
                pods[1].place(3.1, -1.1, -13.5, t, 1);
            }
            case ORBIT_CAM -> {
                double angle = Math.toRadians(-155 + 130 * u);
                double radius = 12.5;
                rig.lookAt(Math.sin(angle) * radius, -3 + 2.2 * u, Math.cos(angle) * radius, hx, hy, hz, 0);
                capsule.place(-5.2, 1.1, 2.4, t, 0);
                pods[0].place(4.2, -1.8, -6 + 3 * Math.sin(t * .5), t, 1);
                pods[1].place(-3.2, 2.6, -11, t, 1);
            }
        }
        if (calm) {
            flow = 30;
            streak = 0;
            lines = 0;
        }
        shipPosition.setX(hx);
        shipPosition.setY(hy);
        shipPosition.setZ(hz);
        shipYaw.setAngle(yaw);
        shipPitch.setAngle(pitch);
        shipRoll.setAngle(roll);
        double throttle = phase == Phase.DEPARTURE ? .6 + .4 * burn : .85;
        exhaust.setScaleZ(1 + throttle * .7 + Math.sin(t * 38) * .12 + Math.sin(t * 61) * .06);
        exhaust.setScaleX(1 + Math.sin(t * 47) * .05);
        exhaust.setScaleY(exhaust.getScaleX());
        capsule.craft.flicker(t, throttle);
        pods[0].craft.flicker(t + 3, 1);
        pods[1].craft.flicker(t + 7, 1);

        double p = progress;
        earth.setVisible(phase == Phase.DEPARTURE || phase == Phase.RETURN
                || (phase == Phase.CRUISE && destination == Destination.MOON));
        target.setVisible(phase == Phase.CRUISE || (phase == Phase.RETURN));
        earthTurn.setAngle(t * 3);
        targetTurn.setAngle(35 + t * 2);
        if (phase == Phase.DEPARTURE) {
            double away = burn;
            earth.setRadius(46);
            earth.setTranslateX(-14);
            earth.setTranslateY(46 + 30 * away);
            earth.setTranslateZ(-30 - 260 * away * away);
            nasaSatellite.setTranslateX(-9 + 2 * Math.sin(t * .2));
            nasaSatellite.setTranslateY(3.5);
            nasaSatellite.setTranslateZ(-16 - 30 * away);
            nasaTurn.setAngle(t * 12);
        } else if (phase == Phase.CRUISE) {
            double approach = ease((p - .14) / .28);
            target.setRadius(9 + 55 * approach);
            target.setTranslateX(18);
            target.setTranslateY(-7);
            target.setTranslateZ(330);
            earth.setRadius(16 * (1 - approach) + 3);
            earth.setTranslateX(-30);
            earth.setTranslateY(18);
            earth.setTranslateZ(-280);
            ringedPlanet.setTranslateX(-190);
            ringedPlanet.setTranslateY(-80);
            ringedPlanet.setTranslateZ(470);
            for (int i = 0; i < rocks.size(); i++) {
                var rock = rocks.get(i);
                double[] home = (double[]) rock.getUserData();
                double z = home[2] - t * (shot == Shot.FLYBY ? 4 : 26);
                z = ((z + 70) % 140 + 140) % 140 - 70;
                rock.setTranslateX(home[0]);
                rock.setTranslateY(home[1]);
                rock.setTranslateZ(z);
                rock.setRotate(t * rockSpin[i]);
            }
        } else {
            double home = ease((p - .90) / .10);
            earth.setRadius(10 + 60 * home);
            earth.setTranslateX(-18);
            earth.setTranslateY(-6);
            earth.setTranslateZ(330);
            target.setRadius(28);
            target.setTranslateX(16);
            target.setTranslateY(20);
            target.setTranslateZ(-200);
        }
        return new double[]{flow, streak, lines};
    }

    private void showOrbit(double p) {
        double t = time;
        double drift = calm ? .2 : 1;
        rig.lookAt(1.6 * Math.sin(t * .11) * drift, .7 * Math.cos(t * .13) * drift, -19, 0, 0, 0, 0);
        earth.setVisible(false);
        target.setVisible(true);
        capsule.root.setVisible(false);
        target.setRadius(10.3);
        target.setTranslateX(6);
        target.setTranslateY(4.9);
        target.setTranslateZ(10);
        targetTurn.setAngle(35 + 105 * p + t * 1.5);
        double orbit = t * .18;
        payload.setTranslateX(.7 + p * 3 + Math.sin(orbit) * .6);
        payload.setTranslateY(-2.25 + Math.cos(orbit * 1.3) * .25);
        payload.setTranslateZ(1);
        payload.setScaleX(.25);
        payload.setScaleY(.25);
        payload.setScaleZ(.25);
        payloadTurn.setAngle(35 + p * 210 + t * 9);
    }

    private void showEntry(double p) {
        double t = time;
        double shake = calm ? 0 : .06;
        earth.setVisible(false);
        target.setVisible(true);
        target.setRadius(60);
        target.setTranslateX(0);
        target.setTranslateY(70 - 18 * ease((p - .42) / .15));
        target.setTranslateZ(30);
        targetTurn.setAngle(40 + t * 2);
        shipPosition.setX(Math.sin(t * 23) * shake);
        shipPosition.setY(Math.cos(t * 19) * shake);
        shipPosition.setZ(0);
        shipYaw.setAngle(0);
        shipPitch.setAngle(28);
        shipRoll.setAngle(calm ? 0 : 6 * Math.sin(t * .9));
        rig.lookAt(5.5, -3.2, -10, 0, .3, 1.5, 0);
        for (int i = 0; i < entryFire.getChildren().size(); i++) {
            var flame = entryFire.getChildren().get(i);
            double pulse = calm ? 1 : 1 + .12 * Math.sin(t * 29 + i * 2.1);
            flame.setScaleX(pulse);
            flame.setScaleY(pulse);
        }
    }

    private void showSurface(double p) {
        rig.lookAt(0, 0, -19, 0, 0, 0, 0);
        earth.setVisible(false);
        target.setVisible(false);
        double touchdown = ease((p - .57) / .12);
        double exploration = Math.max(0, p - .69);
        double idle = calm ? 0 : Math.sin(time * .9) * 4;
        payload.setTranslateX(-.35 + exploration * (destination == Destination.MARS ? 6 : 2));
        payload.setTranslateY(-3.7 + touchdown * 5.8);
        payload.setTranslateZ(-2.5);
        payload.setScaleX(1);
        payload.setScaleY(1);
        payload.setScaleZ(1);
        payloadTurn.setAngle(-34 + exploration * 52 + idle);
        shadow.setTranslateX(payload.getTranslateX());
        shadow.setTranslateY(3.12);
        shadow.setTranslateZ(payload.getTranslateZ() + .3);
        shadow.setOpacity(.12 + touchdown * .33);
    }

    private void buildEntryFire() {
        String[] colors = {"#FFF7C2", "#FFC94A", "#FF8A2A", "#FF4E1F"};
        for (int i = 0; i < colors.length; i++) {
            var flame = new Sphere(1.05 + i * .32, 28);
            flame.setMaterial(ModelLibrary.lit(colors[i], colors[i]));
            flame.setScaleZ(.42 + i * .1);
            flame.setTranslateZ(2.55 - i * .32);
            flame.setOpacity(.88 - i * .17);
            entryFire.getChildren().add(flame);
        }
    }

    private void drawOverlay(double speedLines, double flash, boolean surface) {
        var g = overlay.getGraphicsContext2D();
        double w = overlay.getWidth();
        double h = overlay.getHeight();
        g.clearRect(0, 0, w, h);
        if (w < 2 || h < 2) {
            return;
        }
        g.setFill(new RadialGradient(0, 0, w / 2, h / 2, Math.hypot(w, h) * .62, false, CycleMethod.NO_CYCLE,
                new Stop(.55, Color.TRANSPARENT), new Stop(1, Color.color(0, .01, .06, surface ? .35 : .6))));
        g.fillRect(0, 0, w, h);
        if (speedLines > .02) {
            var random = new SplittableRandom((long) (time * 24));
            double cx = w / 2;
            double cy = h / 2;
            double outer = Math.hypot(w, h) * .6;
            g.setStroke(Color.WHITE);
            for (int i = 0; i < 64; i++) {
                double angle = random.nextDouble(0, Math.TAU);
                double inner = outer * random.nextDouble(.42, .8);
                g.setGlobalAlpha(speedLines * random.nextDouble(.18, .55));
                g.setLineWidth(random.nextDouble(.8, 3.2));
                g.strokeLine(cx + Math.cos(angle) * inner, cy + Math.sin(angle) * inner,
                        cx + Math.cos(angle) * outer, cy + Math.sin(angle) * outer);
            }
            g.setGlobalAlpha(1);
        }
        if (flash > 0) {
            g.setGlobalAlpha(flash * .55);
            g.setFill(Color.web("#DFF8FF"));
            g.fillRect(0, 0, w, h);
            g.setGlobalAlpha(1);
        }
    }

    private static double ease(double fraction) {
        double t = Math.clamp(fraction, 0, 1);
        return t * t * (3 - 2 * t);
    }

    private static Group framed(Group model, double size) {
        var bounds = model.getBoundsInLocal();
        model.setTranslateX(-bounds.getCenterX());
        model.setTranslateY(-bounds.getCenterY());
        model.setTranslateZ(-bounds.getCenterZ());
        double scale = size / Math.max(bounds.getWidth(), Math.max(bounds.getHeight(), bounds.getDepth()));
        var framed = new Group(model);
        framed.getTransforms().add(new Scale(scale, scale, scale));
        return new Group(framed);
    }

    private static Sphere globe(String name, String color) {
        var sphere = new Sphere(1, 96);
        var material = ModelLibrary.surface(color);
        var map = image(name + "-map.jpg");
        if (map != null) {
            material.setDiffuseColor(Color.WHITE);
            material.setDiffuseMap(map);
        }
        material.setSpecularColor(Color.TRANSPARENT);
        sphere.setMaterial(material);
        return sphere;
    }

    private static Image image(String name) {
        var resource = FlightView.class.getResource("/com/liftoff/art/flight/" + name);
        return resource == null ? null : new Image(resource.toExternalForm());
    }


    private static final class Escort {
        final SpaceShips.Craft craft;
        final Group root;
        private final Translate position = new Translate();
        private final Rotate bank = new Rotate(0, Rotate.Z_AXIS);
        private final Rotate spin = new Rotate(0, Rotate.Y_AXIS);

        Escort(SpaceShips.Craft craft, double scale) {
            this.craft = craft;
            Node node = craft.node();
            root = new Group(node);
            root.getTransforms().addAll(position, bank, new Rotate(-90, Rotate.X_AXIS), spin,
                    new Scale(scale, scale, scale));
        }

        void place(double x, double y, double z, double time, double style) {
            position.setX(x);
            position.setY(y);
            position.setZ(z);
            bank.setAngle(style == 0 ? 6 * Math.sin(time * .7) : 14 * Math.sin(time * 1.3 + x));
            spin.setAngle(style == 0 ? time * 22 : time * 90);
        }
    }
}
