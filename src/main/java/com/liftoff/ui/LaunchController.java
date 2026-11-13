package com.liftoff.ui;

import com.liftoff.render.RocketArt;
import com.liftoff.session.MissionStep;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public final class LaunchController extends MissionScreen {
    @FXML private Label status;
    @FXML private Label countdown;
    @FXML private VBox issues;
    @FXML private StackPane rocketArt;
    @FXML private Button check;
    private Timeline timer;
    private WarningList warnings;

    @Override
    protected void setUp() {
        warnings = new WarningList(text);
        issues.getChildren().add(warnings);
        rocketArt.getChildren().add(RocketArt.create(190));
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
    }

    @FXML
    private void onCheck() {
        if (!session.isUnlocked(MissionStep.FLY) && !session.runLaunchCheck().isEmpty()) {
            return;
        }
        check.setDisable(true);
        countdown.setText(text.get("launch.countdown", 3));
        timer = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> countdown.setText(text.get("launch.countdown", 2))),
                new KeyFrame(Duration.seconds(2), event -> countdown.setText(text.get("launch.countdown", 1))),
                new KeyFrame(Duration.seconds(3), event -> onNext()));
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
