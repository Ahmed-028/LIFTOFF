package com.liftoff.ui;

import com.liftoff.app.SceneNavigator;
import com.liftoff.app.Screen;
import com.liftoff.app.ScreenController;
import com.liftoff.data.MissionEntry;
import com.liftoff.session.GameSession;
import com.liftoff.session.MissionStep;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

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
        stepper = new StepperBar(session, text, this::onStep);
        stepperHost.getChildren().setAll(stepper);
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

    @FXML
    protected void onNext() {
        session.next();
        navigator.showMissionStep();
    }
}
