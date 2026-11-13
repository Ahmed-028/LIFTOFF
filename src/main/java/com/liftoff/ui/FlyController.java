package com.liftoff.ui;

import com.liftoff.engine.sim.SimulationResult;
import com.liftoff.session.MissionStep;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.util.Duration;

import java.util.concurrent.ThreadLocalRandom;

public final class FlyController extends MissionScreen {
    @FXML private Label time;
    @FXML private Label telemetry;
    @FXML private Label eventSummary;
    @FXML private ProgressBar progress;
    @FXML private ListView<String> flightLog;
    @FXML private Button playback;
    @FXML private Button skip;
    @FXML private Button report;
    private SimulationResult flight;
    private Timeline timer;
    private int index;

    @Override
    protected void setUp() {
        flight = session.simulation().orElseGet(() -> session.fly(ThreadLocalRandom.current().nextLong()));
        time.setText(text.get("fly.ready"));
        telemetry.setText(text.get("fly.standby"));
        timer = new Timeline(new KeyFrame(Duration.millis(430), event -> showNextStep()));
        timer.setCycleCount(Timeline.INDEFINITE);
        if (flight.timeline().isEmpty()) {
            finish();
        } else {
            timer.play();
        }
    }

    private void showNextStep() {
        var step = flight.timeline().get(index);
        String label = text.get("fly.step", text.get("duration.single." + flight.stepUnit()), index + 1);
        time.setText(label);
        String readings = text.get("fly.telemetry", step.dataProducedGbit(), step.dataSentGbit(),
                step.storageUsedGbit(), step.dataLostGbit(), step.powerShortfallW());
        if (mission().crew() > 0) {
            readings += "\n" + text.get("fly.crew", step.suppliesLeftKg());
        }
        telemetry.setText(readings);
        flightLog.getItems().add(text.get("fly.entry", label, step.dataSentGbit()));
        flight.events().stream().filter(event -> event.timeIndex() == step.index()).forEach(event -> {
            String message = text.get(event.messageKey());
            eventSummary.setText(message);
            flightLog.getItems().add("⚠ " + message);
        });
        flightLog.scrollTo(flightLog.getItems().size() - 1);
        progress.setProgress((double) ++index / flight.timeline().size());
        if (index == flight.timeline().size()) {
            finish();
        }
    }

    @FXML
    private void onPlayback() {
        if (timer.getStatus() == Timeline.Status.RUNNING) {
            timer.pause();
            playback.setText(text.get("fly.resume"));
        } else {
            timer.play();
            playback.setText(text.get("fly.pause"));
        }
    }

    @FXML
    private void onSkip() {
        timer.stop();
        while (index < flight.timeline().size()) {
            showNextStep();
        }
    }

    private void finish() {
        timer.stop();
        playback.setDisable(true);
        skip.setDisable(true);
        report.setDisable(!session.isUnlocked(MissionStep.REPORT));
    }

    @Override
    public void dispose() {
        if (timer != null) {
            timer.stop();
        }
        super.dispose();
    }
}
