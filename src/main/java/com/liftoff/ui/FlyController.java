package com.liftoff.ui;

import com.liftoff.engine.sim.SimulationResult;
import com.liftoff.render3d.FlightView;
import com.liftoff.model.ModuleSlot;
import com.liftoff.session.stub.Mission.Destination;
import com.liftoff.session.MissionStep;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.concurrent.ThreadLocalRandom;
import java.util.EnumMap;

public final class FlyController extends MissionScreen {
    private static final double STEP_MILLIS = 870;

    @FXML private Label time;
    @FXML private Label telemetry;
    @FXML private Label eventSummary;
    @FXML private ProgressBar progress;
    @FXML private ListView<String> flightLog;
    @FXML private Button playback;
    @FXML private Button skip;
    @FXML private Button report;
    @FXML private StackPane viewport;
    @FXML private Label phase;
    @FXML private Label route;
    @FXML private CheckBox calm;
    private SimulationResult flight;
    private Timeline timer;
    private int index;
    private FlightView view;
    private Destination destination;
    private int plannedSteps;

    @Override
    protected void setUp() {
        flight = session.simulation().orElseGet(() -> session.fly(ThreadLocalRandom.current().nextLong()));
        destination = mission().destination();
        plannedSteps = Math.max(1, mission().steps() > 0 ? mission().steps() : session.design().scienceMonths());
        calm.setSelected(navigator.reducedMotion());
        calm.selectedProperty().addListener((property, previous, enabled) -> navigator.setReducedMotion(enabled));
        route.setText(text.get("flight.route." + destination));
        if (Platform.isSupported(ConditionalFeature.SCENE3D)) {
            var models = new EnumMap<ModuleSlot, String>(ModuleSlot.class);
            session.build().parts().forEach((slot, part) -> models.put(ModuleSlot.valueOf(slot.name()),
                    slot.name().equals("SATELLITE") ? PartPreview.modelFor(part) : part.modelId()));
            view = new FlightView(destination, models);
            viewport.getChildren().addFirst(view);
        } else {
            var artwork = new StackPane();
            ObjectiveCard.setArtwork(artwork, mission().id(), 260);
            viewport.getChildren().addFirst(artwork);
        }
        time.setText(text.get("fly.ready"));
        telemetry.setText(text.get("fly.standby"));
        timer = new Timeline(new KeyFrame(Duration.millis(STEP_MILLIS), event -> showNextStep()));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.currentTimeProperty().addListener((property, previous, current) -> {
            if (!calm.isSelected() && index < flight.timeline().size() && current.lessThan(timer.getCycleDuration())) {
                showFlight((index + current.toMillis() / STEP_MILLIS) / plannedSteps);
            }
        });
        showFlight(0);
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
            eventSummary.setText("⚠ " + message + "\n" + text.get("flight.tip." + event.type()));
            eventSummary.setVisible(true);
            eventSummary.setManaged(true);
            flightLog.getItems().add("⚠ " + message);
        });
        flightLog.scrollTo(flightLog.getItems().size() - 1);
        showFlight((double) ++index / plannedSteps);
        if (index == flight.timeline().size()) {
            finish();
        }
    }

    private void showFlight(double fraction) {
        progress.setProgress(fraction);
        String caption = text.get("flight.phase." + FlightView.phase(destination, fraction));
        if (!caption.equals(phase.getText())) {
            phase.setText(caption);
        }
        if (view != null) {
            view.showProgress(fraction);
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
        if (index < plannedSteps) {
            phase.setText(text.get("flight.endedEarly"));
        }
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
