package com.liftoff.render3d;

import com.liftoff.render.RocketArt;
import com.liftoff.render.Theme;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PointLight;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;


public final class AwardScene extends CutsceneView {

    public static final double LENGTH = 13.5;
    private static final double STAGE_TOP = -.6;
    private static final double NOVA_X = -.7;
    private static final double STAGE_Z = 2.2;

    private final RoundFigure nova = new RoundFigure(RoundFigure.Look.GROWN_UP_NOVA);
    private final RoundFigure presenter = new RoundFigure(RoundFigure.Look.PRESENTER);
    private final Group trophy = new Group();
    private final List<RoundFigure> audience = new ArrayList<>();
    private final List<double[]> confettiPaths = new ArrayList<>();
    private final List<Box> confetti = new ArrayList<>();

    public AwardScene() {
        super(Color.web("#050A1C"), Color.web("#2C3558"));
        buildHall();
        buildTrophy();
        buildAudience();
        buildConfetti();
        nova.setRotationAxis(Rotate.Y_AXIS);
        presenter.setRotationAxis(Rotate.Y_AXIS);
        world.getChildren().addAll(nova, presenter, trophy);

        var spotNova = new PointLight(Color.web("#FFF1D6"));
        at(spotNova, -1, -7, -2);
        var spotStage = new PointLight(Color.web("#9FB8FF"));
        at(spotStage, 5, -5, -4);
        var rimLight = new PointLight(Color.web("#22E3F2").deriveColor(0, 1, .6, 1));
        at(rimLight, 0, -3, 4.2);
        world.getChildren().addAll(spotNova, spotStage, rimLight);
        update(0);
    }

    @Override
    public double length() {
        return LENGTH;
    }

    @Override
    public void update(double t) {

        double walk = clamp((t - 2.4) / 3.0);
        nova.setTranslateX(lerp(-7.5, NOVA_X, ease(walk)));
        nova.setTranslateY(STAGE_TOP);
        nova.setTranslateZ(STAGE_Z);
        boolean walking = walk > 0 && walk < 1;
        double faceFront = phase(t, 5.4, .5);
        nova.setRotate(lerpAngle(walk <= 0 ? -90 : -90, 0, faceFront));
        nova.bob(t, walking);


        presenter.setTranslateX(1.9);
        presenter.setTranslateY(STAGE_TOP);
        presenter.setTranslateZ(STAGE_Z + .2);
        double toNova = phase(t, 6.0, .5) - phase(t, 8.4, .5);
        presenter.setRotate(lerp(0, 75, toNova));
        presenter.bob(t, false);
        if (t > 6.4 && t < 8.3) {
            presenter.hands(0, 1);
        }
        if (t > 8.8) {
            presenter.wave(t);
        }


        double[] onPodium = {2.9, STAGE_TOP - 1.42, STAGE_Z - .1};
        double[] held = {1.2, STAGE_TOP - 1.55, STAGE_Z - .3};
        double[] given = {NOVA_X + .2, STAGE_TOP - 1.55, STAGE_Z - .75};
        double[] lifted = {NOVA_X, STAGE_TOP + nova.handsHighY() + .08, STAGE_Z - .15};
        double pick = phase(t, 6.2, .5);
        double pass = phase(t, 7.2, .9);
        double lift = phase(t, 8.6, .6);
        double[] where = new double[3];
        for (int i = 0; i < 3; i++) {
            where[i] = lerp(lerp(lerp(onPodium[i], held[i], pick), given[i], pass), lifted[i], lift);
        }
        at(trophy, where[0], where[1], where[2]);
        trophy.setRotationAxis(Rotate.Y_AXIS);
        trophy.setRotate(t * 25);
        if (pass > .5 && lift <= 0) {
            nova.hands(0, 1);
        }
        if (lift > 0) {
            nova.cheer(t);
        }


        for (int i = 0; i < audience.size(); i++) {
            var person = audience.get(i);
            boolean cheering = t > 8.7 + (i % 5) * .12;
            double hop = cheering ? Math.abs(Math.sin(t * 7 + i * 1.3)) * .22 : Math.abs(Math.sin(t * 1.5 + i)) * .02;
            person.setTranslateY(-hop);
            if (cheering) {
                person.cheer(t + i);
            } else {
                person.bob(t, false);
            }
        }


        double fall = t - 8.7;
        for (int i = 0; i < confetti.size(); i++) {
            var piece = confetti.get(i);
            double[] path = confettiPaths.get(i);
            piece.setVisible(fall > path[3]);
            double since = Math.max(0, fall - path[3]);
            double y = -9 + since * path[4];
            at(piece, path[0] + Math.sin(since * 2 + i) * .4, Math.min(STAGE_TOP - .05, y), path[2]);
            piece.setRotationAxis(new javafx.geometry.Point3D(path[5], 1, path[6]));
            piece.setRotate(since * 300 + i * 40);
        }


        double follow = phase(t, 2.4, 3.6);
        double close = phase(t, 8.4, 1.8);
        double outro = phase(t, 11.0, 2.2);
        double[] hall = {0, -5.6, -11.5, 0, -2.4, 3};
        double[] walkShot = {-1.2, -3.0, -2.5, NOVA_X + .4, -2.2, STAGE_Z};
        double[] celebrate = {NOVA_X + .7, -3.1, -2.4, NOVA_X + .5, -2.9, STAGE_Z};
        double[] cheering = {0, -6.0, -10.0, 0, -2.6, 2};
        double[] shot = new double[6];
        for (int i = 0; i < 6; i++) {
            shot[i] = lerp(lerp(lerp(hall[i], walkShot[i], follow), celebrate[i], close), cheering[i], outro);
        }
        look(shot[0], shot[1], shot[2], shot[3], shot[4], shot[5]);
    }

    private void buildHall() {
        world.getChildren().addAll(
                box(30, .2, 30, 0, .1, 0, "#0D1E42"),
                box(12, .6, 5.2, 0, STAGE_TOP / 2, STAGE_Z + .3, "#21427A"),
                box(12.05, .12, .1, 0, STAGE_TOP + .06, STAGE_Z - 2.3, "#FF7A1A"),
                box(30, 14, .2, 0, -7, 5.4, "#101A3A"));

        for (int side : new int[] {-1, 1}) {
            for (int fold = 0; fold < 4; fold++) {
                var drape = new Cylinder(.45, 12, 20);
                drape.setMaterial(ModelLibrary.surface(fold % 2 == 0 ? "#2D3478" : "#252B66"));
                at(drape, side * (5.6 + fold * .7), -6, 4.6 - fold * .15);
                world.getChildren().add(drape);
            }
        }

        var banner = new MeshView(Meshes.quad(7.2, 2.3));
        var material = new PhongMaterial(Color.WHITE);
        var art = banner();
        material.setDiffuseMap(art);
        material.setSelfIlluminationMap(art);
        banner.setMaterial(material);
        banner.setCullFace(CullFace.NONE);
        at(banner, 0, -6.4, 5.25);
        world.getChildren().add(banner);

        for (int i = -5; i <= 5; i++) {
            world.getChildren().add(at(glow(.12, i % 2 == 0 ? "#FFCB69" : "#22E3F2"), i * 1.1, -8.4, 5.2));
        }

        world.getChildren().addAll(
                box(1.3, 1.4, .9, 2.9, STAGE_TOP - .7, STAGE_Z, "#F2F5FA"),
                box(1.32, .16, .92, 2.9, STAGE_TOP - .95, STAGE_Z, "#FF7A1A"),
                at(glow(.14, "#FFCB69"), 2.9, STAGE_TOP - .45, STAGE_Z - .47));
    }

    private void buildTrophy() {
        var base = new Cylinder(.2, .14, 24);
        base.setMaterial(ModelLibrary.surface("#16305E"));
        at(base, 0, -.07, 0);
        var stem = new Cylinder(.05, .3, 12);
        stem.setMaterial(gold());
        at(stem, 0, -.29, 0);
        var cup = Meshes.view(Meshes.lathe(new double[] {
                .3, -.9, .28, -.78, .22, -.62, .12, -.5, .05, -.44}, 32), gold());
        cup.setCullFace(CullFace.NONE);
        var star = glow(.1, "#FFE7A0");
        at(star, 0, -1.02, 0);
        trophy.getChildren().addAll(base, stem, cup, star);
    }

    private static PhongMaterial gold() {
        var material = ModelLibrary.lit("#F5C842", "#8A6510");
        material.setSpecularColor(Color.web("#FFF2C0"));
        material.setSpecularPower(40);
        return material;
    }


    private void buildAudience() {
        String[] shirts = {"#FF7A1A", "#22A6B5", "#8BE07A", "#B388FF", "#FFCB69", "#3E7FD0", "#FF6F91"};
        String[] legs = {"#16305E", "#2A3040", "#3D4E70", "#0D1E42"};
        String[] skin = {"#F6C9A0", "#E0A97A", "#C68A5C", "#8D5A3B", "#5E3B26"};
        String[] hair = {"#2A1C14", "#5A3A2A", "#C98E4A", "#0B0B0B", "#7A4A2A", "#D9D2C5"};
        var random = new SplittableRandom(12);
        int seat = 0;
        for (int row = 0; row < 3; row++) {
            for (double x = -6.3 + (row % 2) * .65; x <= 6.3; x += 1.3) {
                var look = new RoundFigure.Look(random.nextInt(4) == 0, shirts[seat % shirts.length],
                        shirts[(seat + 3) % shirts.length], legs[random.nextInt(legs.length)],
                        skin[random.nextInt(skin.length)], hair[random.nextInt(hair.length)], false);
                var person = new RoundFigure(look);
                person.setRotationAxis(Rotate.Y_AXIS);
                person.setRotate(180 + random.nextDouble(-12, 12));
                at(person, x, 0, -3.0 - row * 1.25);
                audience.add(person);
                world.getChildren().add(person);
                seat++;
            }
        }
    }

    private void buildConfetti() {
        String[] colours = {"#FF7A1A", "#22E3F2", "#FFCB69", "#F2F5FA", "#8BE07A", "#FF6F91"};
        var random = new SplittableRandom(21);
        for (int i = 0; i < 110; i++) {
            var piece = new Box(.14, .02, .09);
            piece.setMaterial(ModelLibrary.lit(colours[i % colours.length], colours[i % colours.length]));
            piece.setVisible(false);
            confetti.add(piece);
            confettiPaths.add(new double[] {random.nextDouble(-5.5, 5.5), 0, random.nextDouble(-2.5, 4.5),
                    random.nextDouble(0, 2.6), random.nextDouble(1.6, 3.2), random.nextDouble(-1, 1),
                    random.nextDouble(-1, 1)});
            world.getChildren().add(piece);
        }
    }

    private static Image banner() {
        var canvas = new Canvas(720, 230);
        var g = canvas.getGraphicsContext2D();
        g.setFill(Theme.NAVY_700);
        g.fillRoundRect(0, 0, 720, 230, 40, 40);
        g.setStroke(Theme.AMBER);
        g.setLineWidth(8);
        g.strokeRoundRect(8, 8, 704, 214, 34, 34);
        var params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        var rocket = RocketArt.create(170).snapshot(params, null);
        g.drawImage(rocket, 40, 30);
        g.drawImage(rocket, 720 - 40 - rocket.getWidth(), 30);
        g.setTextAlign(TextAlignment.CENTER);
        g.setFill(Theme.AMBER);
        g.setFont(Theme.display(64));
        g.fillText("★ MISSION DESIGN AWARD ★", 360, 105);
        g.setFill(Theme.TEXT);
        g.setFont(Theme.bold(26));
        g.fillText("For outstanding work designing", 360, 150);
        g.fillText("space missions", 360, 184);
        return canvas.snapshot(null, null);
    }
}
