package com.liftoff.app;

import com.liftoff.FxTest;
import com.liftoff.data.CatalogLoader;
import com.liftoff.session.GameSession;
import com.liftoff.session.MissionStep;
import com.liftoff.util.ErrorLog;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** No uncaught exception may reach the player: they get a friendly screen and a way back, and the cause is logged. */
class ErrorScreenTest {

    @TempDir
    Path folder;

    private Path logFile;
    private GameSession session;
    private Scene scene;
    private SceneNavigator navigator;

    @BeforeEach
    void setUp() {
        logFile = folder.resolve("error.log");
        FxTest.run(() -> {
            session = new GameSession(CatalogLoader.loadFromClasspath());
            scene = new Scene(new StackPane(), 1280, 800);
            navigator = new SceneNavigator(scene, session,
                    ResourceBundle.getBundle(LiftoffApp.MESSAGES_BUNDLE, Locale.ENGLISH), url -> { },
                    new ErrorLog(logFile));
        });
    }

    /** Scroll panes only attach their content once styles are applied, so look after a layout pass. */
    private javafx.scene.Node find(String selector) {
        scene.getRoot().applyCss();
        scene.getRoot().layout();
        return scene.lookup(selector);
    }

    private void assertFriendlyScreen() {
        assertNotNull(scene.lookup("#error-screen"), "the error screen is not showing");
        Label title = (Label) scene.lookup("#error-title");
        Label body = (Label) scene.lookup("#error-body");
        assertEquals("Something went wrong", title.getText());
        assertTrue(body.getText().contains("go back"), body.getText());
        assertFalse(body.getText().contains("Exception"), "no stack trace talk on the player's screen");
        assertNotNull(title.getGraphic(), "the title carries an icon, not colour alone");
    }

    @Test
    void anErrorShowsTheFriendlyScreenAndLogsTheCause() {
        FxTest.run(() -> {
            navigator.show(Screen.TITLE);
            navigator.showError(new IllegalStateException("boom in a handler"));

            assertFriendlyScreen();
            assertTrue(Files.readString(logFile).contains("IllegalStateException: boom in a handler"));
        });
    }

    @Test
    void goBackReturnsToTheMissionListAndTheGameCarriesOn() {
        FxTest.run(() -> {
            navigator.show(Screen.TITLE);
            navigator.showError(new RuntimeException("boom"));

            ((Button) scene.lookup("#error-back")).fire();

            assertNull(scene.lookup("#error-screen"));
            assertNotNull(find("#mission-farm-watch"), "the mission list is showing");
            ((Button) find("#mission-farm-watch")).fire();
            assertEquals(MissionStep.BRIEFING, session.step());
        });
    }

    @Test
    void aScreenThatFailsToOpenBecomesTheFriendlyScreenInsteadOfAnException() {
        FxTest.run(() -> {
            navigator.show(Screen.TITLE);
            // The report screen has nothing to show: no mission was flown.
            navigator.show(Screen.REPORT);

            assertFriendlyScreen();
            assertTrue(Files.readString(logFile).contains("ReportController"));
        });
    }

    @Test
    void theGlobalHandlerCatchesWhatNothingElseDid() {
        FxTest.run(() -> {
            navigator.show(Screen.TITLE);
            Thread.UncaughtExceptionHandler before = Thread.getDefaultUncaughtExceptionHandler();
            try {
                LiftoffApp.installCrashHandler(navigator);
                Thread.getDefaultUncaughtExceptionHandler()
                        .uncaughtException(Thread.currentThread(), new ArithmeticException("divide by zero"));

                assertFriendlyScreen();
                assertTrue(Files.readString(logFile).contains("ArithmeticException: divide by zero"));
            } finally {
                Thread.setDefaultUncaughtExceptionHandler(before);
            }
        });
    }

    @Test
    void aSecondErrorWhileTheScreenIsUpStillLeavesAWayBack() {
        FxTest.run(() -> {
            navigator.showError(new RuntimeException("first"));
            navigator.showError(new RuntimeException("second"));

            assertFriendlyScreen();
            String log = Files.readString(logFile);
            assertTrue(log.contains("first") && log.contains("second"));
            ((Button) scene.lookup("#error-back")).fire();
            assertNotNull(find("#mission-farm-watch"));
        });
    }
}
