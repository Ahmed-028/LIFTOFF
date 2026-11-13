package com.liftoff.ui;

import com.liftoff.app.SceneNavigator;
import com.liftoff.app.Screen;
import com.liftoff.app.ScreenController;
import com.liftoff.data.MissionEntry;
import com.liftoff.session.GameSession;
import com.liftoff.session.MissionStep;
import javafx.application.Platform;
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
    private final Runnable onSessionChange = this::queueRefresh;
    private boolean refreshQueued;
    private boolean disposed;

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
        disposed = true;
        session.removeListener(onSessionChange);
    }

    /** True once the screen has been replaced; late callbacks must then do nothing. */
    protected final boolean isDisposed() {
        return disposed;
    }

    /**
     * One action can change the session several times (go to a step, then confirm). The screen is
     * redrawn once, on the next pulse, however many times it was told.
     */
    private void queueRefresh() {
        if (refreshQueued || disposed) {
            return;
        }
        refreshQueued = true;
        Platform.runLater(this::settle);
    }

    @Override
    public final void settle() {
        if (refreshQueued && !disposed) {
            refreshQueued = false;
            refresh();
        }
    }

    protected MissionEntry mission() {
        return session.catalog().mission(session.design().objective().id()).orElseThrow();
    }

    protected abstract void setUp();

    protected void update() {}

    protected void onStep(MissionStep step) {
        move(() -> session.goTo(step));
    }

    /**
     * Makes a move in the mission flow and shows the step the session ends up on. A move the flow
     * refuses (the step is locked) leaves the player where they are: it is not an error.
     */
    protected final void move(Runnable change) {
        try {
            change.run();
        } catch (IllegalStateException locked) {
            // The session did not move; the screen for its current step is shown again below.
        }
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
        move(session::back);
    }

    @FXML
    protected void onNext() {
        move(session::next);
    }
}
