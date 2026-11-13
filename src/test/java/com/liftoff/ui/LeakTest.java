package com.liftoff.ui;

import com.liftoff.FxTest;
import com.liftoff.app.LiftoffApp;
import com.liftoff.app.SceneNavigator;
import com.liftoff.app.Screen;
import com.liftoff.data.CatalogLoader;
import com.liftoff.session.GameSession;
import com.liftoff.util.ErrorLog;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Opens every screen, one after the other, 100 times. Nothing may pile up: the session's listeners
 * and each screen's node count stay flat, and closed screens can be garbage collected.
 */
class LeakTest {

    private static final int ROUNDS = 100;
    private static final Screen[] SCREENS = Screen.values();

    private static int nodes(Node node) {
        int count = 1;
        if (node instanceof SubScene sub) {
            count += nodes(sub.getRoot());
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                count += nodes(child);
            }
        }
        return count;
    }

    @Test
    void cyclingThroughEveryScreenAHundredTimesLeavesNothingBehind() {
        GameSession[] session = new GameSession[1];
        Scene[] scene = new Scene[1];
        SceneNavigator[] navigator = new SceneNavigator[1];
        List<WeakReference<Object>> closed = new ArrayList<>();
        int[][] nodeCounts = new int[ROUNDS][SCREENS.length];
        int[] listeners = new int[ROUNDS];

        FxTest.run(() -> {
            session[0] = flownFarmWatch();
            scene[0] = new Scene(new StackPane(), 1280, 800);
            navigator[0] = new SceneNavigator(scene[0], session[0],
                    ResourceBundle.getBundle(LiftoffApp.MESSAGES_BUNDLE, Locale.ENGLISH), url -> { },
                    new ErrorLog(null));
        });
        for (int round = 0; round < ROUNDS; round++) {
            int r = round;
            FxTest.run(() -> {
                for (int i = 0; i < SCREENS.length; i++) {
                    navigator[0].show(SCREENS[i]);
                    assertNull(scene[0].lookup("#error-screen"), SCREENS[i] + " failed to open");
                    scene[0].getRoot().applyCss();
                    scene[0].getRoot().layout();
                    nodeCounts[r][i] = nodes(scene[0].getRoot());
                    closed.add(new WeakReference<>(navigator[0].current()));
                    if (navigator[0].current() instanceof ShipBuilderController builder) {
                        closed.add(new WeakReference<>(builder.shipView()));
                    }
                }
                listeners[r] = session[0].listenerCount();
            });
            FxTest.settle();
        }

        for (int round = 1; round < ROUNDS; round++) {
            assertEquals(listeners[1], listeners[round], "session listeners after round " + round);
            for (int i = 0; i < SCREENS.length; i++) {
                assertEquals(nodeCounts[1][i], nodeCounts[round][i], SCREENS[i] + " node count in round " + round);
            }
        }
        assertTrue(listeners[ROUNDS - 1] <= 1, "only the screen showing listens: " + listeners[ROUNDS - 1]);

        FxTest.run(() -> navigator[0].show(Screen.TITLE));
        FxTest.settle();
        int alive = 0;
        for (int attempt = 0; attempt < 20; attempt++) {
            System.gc();
            FxTest.settle();
            alive = (int) closed.stream().filter(ref -> ref.get() != null).count();
            if (alive <= 3) {
                break;
            }
        }
        assertTrue(alive <= 3, alive + " of " + closed.size() + " closed screens are still held in memory");
    }

    /** A Farm Watch that has flown, so every screen up to the report can open. */
    private static GameSession flownFarmWatch() {
        GameSession session = new GameSession(CatalogLoader.loadFromClasspath());
        session.selectMission("farm-watch");
        session.next();
        List.of("eo1-class", "mr-106", "pumpkin-clamshell", "bay-6u", "cubesat-3u").forEach(session::choosePart);
        session.next();
        session.addInstrument("ali");
        session.chooseOrbit("sso-705");
        session.toggleStation("svalbard");
        session.chooseLaunchVehicle("atlas-v-401");
        session.next();
        session.confirmBuild();
        session.next();
        session.runLaunchCheck();
        session.next();
        session.fly(1);
        session.next();
        return session;
    }
}
