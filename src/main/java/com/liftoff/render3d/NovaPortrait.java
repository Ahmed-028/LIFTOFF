package com.liftoff.render3d;

import javafx.animation.AnimationTimer;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;

/**
 * A small 3D window with Nova from the waist up, for the guide's speech bubble. Nova bobs
 * gently and waves each time {@link #talk()} is called. Shows nothing where 3D is unavailable.
 */
public final class NovaPortrait extends StackPane {

    private final RoundFigure nova = new RoundFigure(RoundFigure.Look.KID_NOVA);
    private final AnimationTimer clock;
    private long start;
    private double talkUntil;
    private double now;

    public NovaPortrait(double width, double height) {
        setMinSize(width, height);
        setPrefSize(width, height);
        setMaxSize(width, height);
        getStyleClass().add("nova-portrait");
        if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
            clock = null;
            return;
        }
        var camera = new PerspectiveCamera(true);
        camera.setNearClip(.05);
        camera.setFarClip(40);
        camera.setFieldOfView(30);
        camera.setTranslateY(-1.75);
        camera.setTranslateZ(-5.4);
        nova.setRotationAxis(Rotate.Y_AXIS);
        nova.setRotate(14);
        var light = new PointLight(Color.web("#FFF1DE"));
        light.setTranslateX(-3);
        light.setTranslateY(-4);
        light.setTranslateZ(-5);
        var world = new Group(nova, camera, light, new AmbientLight(Color.web("#56628A")));
        var scene = new SubScene(world, width, height, true, SceneAntialiasing.BALANCED);
        scene.setCamera(camera);
        scene.setFill(Color.TRANSPARENT);
        scene.setMouseTransparent(true);
        getChildren().add(scene);
        clock = new AnimationTimer() {
            @Override
            public void handle(long time) {
                if (start == 0) {
                    start = time;
                }
                now = (time - start) / 1e9;
                nova.bob(now, false);
                nova.body.setTranslateY(-Math.abs(Math.sin(now * 2.4)) * .03);
                if (now < talkUntil) {
                    nova.wave(now);
                }
            }
        };
        sceneProperty().addListener((property, before, scene2) -> {
            if (scene2 == null) {
                clock.stop();
            } else {
                clock.start();
            }
        });
    }

    /** Nova waves for a moment, as if starting to speak. */
    public void talk() {
        talkUntil = now + 1.3;
    }
}
