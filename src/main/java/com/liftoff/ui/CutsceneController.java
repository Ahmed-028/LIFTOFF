package com.liftoff.ui;

import com.liftoff.app.Cutscene;
import com.liftoff.app.SceneNavigator;
import com.liftoff.app.ScreenController;
import com.liftoff.render3d.AwardScene;
import com.liftoff.render3d.CutsceneView;
import com.liftoff.render3d.IntroScene;
import com.liftoff.render3d.MissionScene;
import com.liftoff.session.GameSession;
import javafx.animation.AnimationTimer;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.ResourceBundle;


public final class CutsceneController implements ScreenController {

    @FXML private StackPane stage;
    @FXML private Label caption;
    @FXML private Button skip;
    @FXML private Region fade;

    private SceneNavigator navigator;
    private UiText text;
    private CutsceneView view;
    private String captionPrefix;

    private String captionFallback;
    private double[] captionTimes;
    private Object[] captionValues = new Object[0];
    private long start;
    private boolean finished;
    private int shownCaption = -1;

    private java.util.List<java.util.Map.Entry<Double, com.liftoff.audio.Sfx>> cues = java.util.List.of();
    private int nextCue;
    private final EventHandler<KeyEvent> keys = this::onKey;

    private final AnimationTimer clock = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (start == 0) {
                start = now;
            }
            show((now - start) / 1e9);
        }
    };

    @Override
    public void attach(SceneNavigator navigator, GameSession session, ResourceBundle messages) {
        this.navigator = navigator;
        this.text = new UiText(messages);
        if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
            Platform.runLater(this::finish);
            return;
        }
        Cutscene kind = navigator.cutscene();
        switch (kind) {
            case INTRO -> {
                view = new IntroScene();
                captionPrefix = "intro.caption.";
                captionTimes = new double[] {0, 2.2, 5.1, 7.8};
                cues = java.util.List.of(java.util.Map.entry(.6, com.liftoff.audio.Sfx.CLICK),
                        java.util.Map.entry(5.3, com.liftoff.audio.Sfx.POP),
                        java.util.Map.entry(7.3, com.liftoff.audio.Sfx.STARTUP),
                        java.util.Map.entry(8.2, com.liftoff.audio.Sfx.TYPE),
                        java.util.Map.entry(10.2, com.liftoff.audio.Sfx.WHOOSH));
            }
            case MISSION -> {
                var mission = session.catalog().mission(session.design().objective().id()).orElseThrow();
                view = new MissionScene(mission.destination());
                String specific = "cutscene.mission." + mission.id() + ".";
                captionPrefix = specific;
                captionFallback = "cutscene.mission.default.";
                captionTimes = new double[] {0, 2.4, 4.8};
                captionValues = new Object[] {mission.briefing().title()};
                cues = java.util.List.of(java.util.Map.entry(3.0, com.liftoff.audio.Sfx.POP),
                        java.util.Map.entry(4.7, com.liftoff.audio.Sfx.STAR),
                        java.util.Map.entry(5.2, com.liftoff.audio.Sfx.WHOOSH));
            }
            case MISSION_END -> {
                var mission = session.catalog().mission(session.design().objective().id()).orElseThrow();
                boolean worked = session.simulation().map(com.liftoff.engine.sim.SimulationResult::success).orElse(false);
                view = new com.liftoff.render3d.ArrivalScene(mission.destination(), worked);
                String outcome = worked ? "success." : "failure.";
                String specific = "cutscene.end." + mission.id() + "." + outcome;
                captionPrefix = specific;
                captionFallback = "cutscene.end.default." + outcome;
                captionTimes = new double[] {0, 3.0, 5.4};
                captionValues = new Object[] {mission.briefing().title()};
                cues = java.util.List.of(java.util.Map.entry(.3, com.liftoff.audio.Sfx.WHOOSH),
                        java.util.Map.entry(2.8, com.liftoff.audio.Sfx.ARRIVE),
                        java.util.Map.entry(4.5, worked ? com.liftoff.audio.Sfx.WIN : com.liftoff.audio.Sfx.LOSE),
                        java.util.Map.entry(4.7, worked ? com.liftoff.audio.Sfx.SPARKLE_3 : com.liftoff.audio.Sfx.CLICK));
            }
            case ENDING -> {
                view = new AwardScene();
                captionPrefix = "cutscene.ending.";
                captionTimes = new double[] {0, 2.4, 6.0, 9.2};
                cues = java.util.List.of(java.util.Map.entry(7.2, com.liftoff.audio.Sfx.PICK),
                        java.util.Map.entry(8.6, com.liftoff.audio.Sfx.WIN),
                        java.util.Map.entry(8.7, com.liftoff.audio.Sfx.CHEER),
                        java.util.Map.entry(8.8, com.liftoff.audio.Sfx.POP));
            }
        }
        view.setAccessibleText(text.get("cutscene.accessible." + kind.name()));
        stage.getChildren().add(view);
        skip.sceneProperty().addListener((property, previous, now) -> {
            if (now != null) {
                now.addEventFilter(KeyEvent.KEY_PRESSED, keys);
                skip.requestFocus();
            }
            if (previous != null) {
                previous.removeEventFilter(KeyEvent.KEY_PRESSED, keys);
            }
        });
        show(0);
        clock.start();
    }


    public void show(double seconds) {
        if (finished || view == null) {
            return;
        }
        double length = view.length();
        view.update(Math.min(seconds, length));
        while (nextCue < cues.size() && seconds >= cues.get(nextCue).getKey()) {
            com.liftoff.audio.Audio.get().play(cues.get(nextCue++).getValue());
        }
        int index = 0;
        for (int i = 0; i < captionTimes.length; i++) {
            if (seconds >= captionTimes[i]) {
                index = i;
            }
        }
        if (index != shownCaption) {
            shownCaption = index;
            String key = captionPrefix + index;
            if (!text.bundle().containsKey(key) && captionFallback != null) {
                key = captionFallback + index;
            }
            caption.setText(text.get(key, captionValues));
            caption.setAccessibleText(caption.getText());
        }
        fade.setOpacity(Math.max(0, Math.min(1, (seconds - (length - .7)) / .7)));
        if (seconds >= length) {
            finish();
        }
    }

    private void onKey(KeyEvent event) {
        switch (event.getCode()) {
            case ENTER, SPACE, ESCAPE -> {
                event.consume();
                finish();
            }
            default -> {
            }
        }
    }

    @FXML
    private void onSkip() {
        finish();
    }

    private void finish() {
        if (finished) {
            return;
        }
        finished = true;
        clock.stop();
        navigator.finishCutscene();
    }

    @Override
    public void dispose() {
        finished = true;
        clock.stop();
        if (skip.getScene() != null) {
            skip.getScene().removeEventFilter(KeyEvent.KEY_PRESSED, keys);
        }
    }
}
