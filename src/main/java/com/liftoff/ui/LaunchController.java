package com.liftoff.ui;

import com.liftoff.session.MissionStep;
import com.liftoff.render.RocketArt;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public final class LaunchController extends MissionScreen {
    @FXML private Label status;
    @FXML private Label countdown;
    @FXML private VBox issues;
    @FXML private StackPane rocketArt;
    @FXML private Button check;
    @FXML private Tab cabinTab;
    private Timeline timer;
    private WarningList warnings;
    private CockpitPanel cockpit;

    @Override
    protected void setUp() {
        warnings = new WarningList(text);
        issues.getChildren().add(warnings);
        BuiltShipView.create(session, text, 320).ifPresentOrElse(ship -> {
            rocketArt.getStyleClass().remove("launch-pad");
            rocketArt.getChildren().add(ship);
        }, () -> rocketArt.getChildren().add(RocketArt.create(190)));
        cockpit = new CockpitPanel(session, text, false, text.get("flight.route." + mission().destination()));
        cockpit.addControl(check);
        cabinTab.setContent(cockpit);
    }

    @Override
    protected void update() {
        warnings.showLaunch(session.launchIssues());
        if (!session.launchIssues().isEmpty()) {
            status.setText(text.get("launch.blocked"));
            check.setDisable(true);
        } else if (session.isUnlocked(MissionStep.FLY)) {
            status.setText(text.get("launch.ready"));
            check.setText(text.get("launch.start"));
        } else {
            status.setText(text.get("launch.waiting"));
        }
        int steps = mission().steps() > 0 ? mission().steps() : session.design().scienceMonths();
        cockpit.update(text.get("cockpit.onPad"), text.get("cockpit.eta", steps,
                text.get("duration." + mission().stepUnit())), 0, false);
        cockpit.telemetry(status.getText());
    }

    @FXML
    private void onCheck() {
        if (!session.isUnlocked(MissionStep.FLY) && !session.runLaunchCheck().isEmpty()) {
            com.liftoff.audio.Audio.get().play(com.liftoff.audio.Sfx.ERROR);
            return;
        }
        check.setDisable(true);
        countdown.setText(text.get("launch.countdown", 3));
        com.liftoff.audio.Audio.get().play(com.liftoff.audio.Sfx.BEEP);
        timer = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    countdown.setText(text.get("launch.countdown", 2));
                    com.liftoff.audio.Audio.get().play(com.liftoff.audio.Sfx.BEEP);
                }),
                new KeyFrame(Duration.seconds(2), event -> {
                    countdown.setText(text.get("launch.countdown", 1));
                    com.liftoff.audio.Audio.get().play(com.liftoff.audio.Sfx.BEEP);
                }),
                new KeyFrame(Duration.seconds(3), event -> {
                    com.liftoff.audio.Audio.get().play(com.liftoff.audio.Sfx.BEEP_HIGH);
                    com.liftoff.audio.Audio.get().play(com.liftoff.audio.Sfx.LIFTOFF);
                    onNext();
                }));
        timer.play();
    }

    @Override
    public void dispose() {
        if (timer != null) {
            timer.stop();
        }
        super.dispose();
    }
}
