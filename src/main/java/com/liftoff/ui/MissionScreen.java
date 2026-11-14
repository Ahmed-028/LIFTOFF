package com.liftoff.ui;

import com.liftoff.app.SceneNavigator;
import com.liftoff.app.Screen;
import com.liftoff.app.ScreenController;
import com.liftoff.data.MissionEntry;
import com.liftoff.session.GameSession;
import com.liftoff.session.Level;
import com.liftoff.session.MissionStep;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.List;
import java.util.ResourceBundle;

/** Common navigation and session-listener lifetime for mission screens. */
public abstract class MissionScreen implements ScreenController {
    @FXML private StackPane stepperHost;

    protected SceneNavigator navigator;
    protected GameSession session;
    protected UiText text;
    private StepperBar stepper;
    private final Runnable onSessionChange = this::refresh;

    @Override
    public final void attach(SceneNavigator navigator, GameSession session, ResourceBundle messages) {
        this.navigator = navigator;
        this.session = session;
        text = new UiText(messages);
        stepper = new StepperBar(session, text, this::onStep, level());
        if (showsStepper() && stepper.hasSteps()) {
            stepperHost.getChildren().setAll(stepper);
        }
        var entry = mission();
        MissionWorld.apply(stepperHost, entry.destination());
        if (stepperHost.getParent() instanceof HBox commandBar && !commandBar.getChildren().isEmpty()) {
            var chip = new Label(text.get("level.chip", entry.briefing().title(), level().number(),
                    Level.values().length));
            chip.getStyleClass().add("mission-chip");
            chip.setMinWidth(Region.USE_PREF_SIZE);
            commandBar.getChildren().add(1, chip);
            if (showsMapButton()) {
                var map = new Button(text.get("map.open"));
                map.setId("open-map");
                map.getStyleClass().add("secondary");
                map.setTooltip(new Tooltip(text.get("map.openHint")));
                map.setMinWidth(Region.USE_PREF_SIZE);
                map.setOnAction(event -> navigator.showRoadMap(List.of()));
                commandBar.getChildren().add(1, map);
            }
            commandBar.getChildren().add(SettingsMenu.create(text, navigator));
        }
        setUp();
        refresh();
        session.addListener(onSessionChange);
    }

    @Override
    public void dispose() {
        session.removeListener(onSessionChange);
    }

    protected MissionEntry mission() {
        return session.catalog().mission(session.design().objective().id()).orElseThrow();
    }

    protected abstract void setUp();

    /** The road-map level this screen belongs to. */
    protected Level level() {
        return Level.of(session.step());
    }

    /** Whether the command bar shows the step pills of the level. */
    protected boolean showsStepper() {
        return true;
    }

    /** Whether the command bar offers the road map; the road map itself does not. */
    protected boolean showsMapButton() {
        return true;
    }

    protected void update() {}

    protected void onStep(MissionStep step) {
        session.goTo(step);
        navigator.showMissionStep();
    }

    private void refresh() {
        stepper.refresh();
        update();
    }

    @FXML
    protected void onMissions() {
        navigator.show(Screen.MISSION_SELECT);
    }

    @FXML
    protected void onBack() {
        session.back();
        navigator.showMissionStep();
    }

    /** Moves to the next step; leaving the level's last step sends the ship back to the road map. */
    @FXML
    protected void onNext() {
        var before = Level.of(session.step());
        boolean flown = session.step() == com.liftoff.session.MissionStep.FLY;
        session.next();
        if (flown && session.step() == com.liftoff.session.MissionStep.REPORT) {
            // Between the flight and the report: the end-of-mission cutscene.
            navigator.playCutscene(com.liftoff.app.Cutscene.MISSION_END, navigator::showMissionStep);
        } else if (Level.of(session.step()) != before) {
            navigator.finishLevel(before);
        } else {
            navigator.showMissionStep();
        }
    }
}
