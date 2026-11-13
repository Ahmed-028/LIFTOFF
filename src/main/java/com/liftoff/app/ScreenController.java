package com.liftoff.app;

import com.liftoff.session.GameSession;

import java.util.ResourceBundle;

/** What every screen controller receives once its FXML has been loaded. */
public interface ScreenController {

    /** Called once, right after loading, with everything the screen needs. */
    void attach(SceneNavigator navigator, GameSession session, ResourceBundle messages);

    /** Called when the screen is replaced, so it can stop listening to the session. */
    default void dispose() {
    }
}
