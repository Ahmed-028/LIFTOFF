package com.liftoff.app;

import com.liftoff.FxTest;
import com.liftoff.data.CatalogLoader;
import com.liftoff.session.GameSession;
import com.liftoff.util.ErrorLog;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The title screen comes up before the game data has loaded, and lets the player in once it has. */
class StartupTest {

    private static Button start(Scene scene) {
        scene.getRoot().applyCss();
        scene.getRoot().layout();
        return (Button) scene.lookup("#start");
    }

    @Test
    void theTitleShowsAtOnceAndItsButtonWaitsForTheGameData() {
        FxTest.run(() -> {
            Scene scene = new Scene(new StackPane(), 1280, 800);
            SceneNavigator navigator = new SceneNavigator(scene,
                    ResourceBundle.getBundle(LiftoffApp.MESSAGES_BUNDLE, Locale.ENGLISH), url -> { },
                    new ErrorLog(null));

            navigator.show(Screen.TITLE);

            assertNull(scene.lookup("#error-screen"));
            assertFalse(navigator.isReady());
            assertTrue(start(scene).isDisabled(), "nothing to start until the missions are loaded");

            navigator.setSession(new GameSession(CatalogLoader.loadFromClasspath()));

            assertTrue(navigator.isReady());
            assertFalse(start(scene).isDisabled());
            start(scene).fire();
            start(scene);   // lays the new screen out
            assertNotNull(scene.lookup("#mission-farm-watch"));
        });
    }

    @Test
    void aTitleOpenedAfterTheDataIsReadyCanStartAtOnce() {
        FxTest.run(() -> {
            Scene scene = new Scene(new StackPane(), 1280, 800);
            new SceneNavigator(scene, new GameSession(CatalogLoader.loadFromClasspath()),
                    ResourceBundle.getBundle(LiftoffApp.MESSAGES_BUNDLE, Locale.ENGLISH)).show(Screen.TITLE);

            assertFalse(start(scene).isDisabled());
        });
    }
}
