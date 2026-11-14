package com.liftoff.ui;

import com.liftoff.app.SceneNavigator;
import com.liftoff.app.Screen;
import com.liftoff.app.ScreenController;
import com.liftoff.render.RocketArt;
import com.liftoff.render.Theme;
import com.liftoff.session.GameSession;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.util.Duration;

import java.util.ResourceBundle;

/**
 * The title screen: the game's name and the way into a mission. Straight after the intro
 * cutscene the rocket lifts off, then the title appears and a new rocket rolls onto the pad.
 */
public final class TitleController implements ScreenController {

    private static final double ROCKET_HEIGHT = 300;

    @FXML private StackPane artHolder;
    @FXML private Label name;
    @FXML private Label tagline;
    @FXML private Label note;
    @FXML private Button start;
    @FXML private Button replay;

    private SceneNavigator navigator;
    private Node rocket;
    private Animation launch;

    @FXML
    private void initialize() {
        rocket = RocketArt.create(ROCKET_HEIGHT);
        artHolder.getChildren().add(rocket);
    }

    @Override
    public void attach(SceneNavigator navigator, GameSession session, ResourceBundle messages) {
        this.navigator = navigator;
        if (navigator.takeTitleLaunch() && !navigator.reducedMotion()) {
            playLaunch();
        }
    }

    private void playLaunch() {
        Node[] texts = {name, tagline, note, start, replay};
        for (Node node : texts) {
            node.setOpacity(0);
        }
        var flame = new Ellipse(0, 0, 34, 70);
        flame.setFill(new RadialGradient(0, 0, .5, .25, .7, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE), new Stop(.35, Theme.AMBER), new Stop(.75, Theme.ORANGE),
                new Stop(1, Color.TRANSPARENT)));
        flame.setEffect(new GaussianBlur(6));
        flame.setTranslateY(ROCKET_HEIGHT / 2 + 30);
        flame.setScaleY(.2);
        artHolder.getChildren().remove(rocket);
        // A stack keeps the flame centred under the rocket however the art is drawn.
        var ship = new StackPane(flame, rocket);
        ship.setMouseTransparent(true);
        artHolder.getChildren().add(ship);

        var smoke = new Group();
        smoke.setMouseTransparent(true);
        smoke.setTranslateY(ROCKET_HEIGHT / 2);
        artHolder.getChildren().add(0, smoke);
        var puffs = new ParallelTransition();
        for (int i = 0; i < 9; i++) {
            double side = (i - 4) * 26;
            var puff = new Circle(side, 0, 26, Color.web("#DCE8F7", .85));
            puff.setScaleX(.2);
            puff.setScaleY(.2);
            smoke.getChildren().add(puff);
            var grow = new ScaleTransition(Duration.millis(1500), puff);
            grow.setDelay(Duration.millis(250 + Math.abs(i - 4) * 70));
            grow.setToX(2.2);
            grow.setToY(1.7);
            var drift = new TranslateTransition(Duration.millis(1500), puff);
            drift.setDelay(grow.getDelay());
            drift.setByX(side * 1.4);
            drift.setByY(-10);
            var fade = new FadeTransition(Duration.millis(1100), puff);
            fade.setDelay(Duration.millis(1400));
            fade.setToValue(0);
            puffs.getChildren().addAll(grow, drift, fade);
        }

        com.liftoff.audio.Audio.get().play(com.liftoff.audio.Sfx.LIFTOFF);
        var ignite = new ScaleTransition(Duration.millis(700), flame);
        ignite.setToY(1);
        var shake = new TranslateTransition(Duration.millis(50), rocket);
        shake.setFromX(-3);
        shake.setToX(3);
        shake.setAutoReverse(true);
        shake.setCycleCount(14);
        var rumble = new ParallelTransition(ignite, shake);

        var climb = new TranslateTransition(Duration.millis(1700), ship);
        climb.setToY(-1100);
        climb.setInterpolator(Interpolator.EASE_IN);
        var stretch = new ScaleTransition(Duration.millis(1700), flame);
        stretch.setToY(1.8);
        var liftoff = new ParallelTransition(climb, stretch);

        var reveal = new ParallelTransition();
        for (int i = 0; i < texts.length; i++) {
            var fadeIn = new FadeTransition(Duration.millis(420), texts[i]);
            fadeIn.setDelay(Duration.millis(i * 120));
            fadeIn.setToValue(1);
            var rise = new TranslateTransition(Duration.millis(420), texts[i]);
            rise.setDelay(fadeIn.getDelay());
            rise.setFromY(18);
            rise.setToY(0);
            reveal.getChildren().addAll(fadeIn, rise);
        }

        var nextRocket = new SequentialTransition(new PauseTransition(Duration.millis(400)));
        nextRocket.setOnFinished(event -> {
            artHolder.getChildren().removeAll(ship, smoke);
            rocket = RocketArt.create(ROCKET_HEIGHT);
            rocket.setOpacity(0);
            rocket.setTranslateY(40);
            artHolder.getChildren().add(rocket);
            var fadeIn = new FadeTransition(Duration.millis(500), rocket);
            fadeIn.setToValue(1);
            var settle = new TranslateTransition(Duration.millis(500), rocket);
            settle.setToY(0);
            new ParallelTransition(fadeIn, settle).play();
            start.requestFocus();
        });

        var whole = new SequentialTransition(new ParallelTransition(new SequentialTransition(rumble, liftoff), puffs),
                reveal, nextRocket);
        launch = whole;
        whole.play();
    }

    @Override
    public void dispose() {
        if (launch != null) {
            launch.stop();
        }
    }

    @FXML
    private void onStart() {
        navigator.show(Screen.MISSION_SELECT);
    }

    @FXML
    private void onIntro() {
        navigator.playIntro();
    }
}
