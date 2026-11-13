package com.liftoff.ui;

import com.liftoff.FxTest;
import com.liftoff.app.LiftoffApp;
import com.liftoff.app.SceneNavigator;
import com.liftoff.app.Screen;
import com.liftoff.data.CatalogLoader;
import com.liftoff.model.ModuleSlot;
import com.liftoff.render3d.HangarShipView;
import com.liftoff.session.GameSession;
import com.liftoff.session.MissionStep;
import com.liftoff.util.ErrorLog;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Drives the real builder screen (real FXML, real catalog, real session) without showing a window.
 * What this cannot do is perform an operating-system drag gesture or render a frame: the drop is
 * exercised through the same method the drop handler calls.
 */
class ShipBuilderScreenTest {

    private static final List<String> FARM_PARTS =
            List.of("eo1-class", "mr-106", "pumpkin-clamshell", "bay-6u", "cubesat-3u");

    private GameSession session;
    private Scene scene;
    private SceneNavigator navigator;

    @BeforeEach
    void openTheBuilder() {
        FxTest.run(() -> {
            session = new GameSession(CatalogLoader.loadFromClasspath());
            scene = new Scene(new StackPane(), 1280, 800);
            scene.getStylesheets().add(LiftoffApp.class.getResource(LiftoffApp.STYLESHEET).toExternalForm());
            navigator = new SceneNavigator(scene, session,
                    ResourceBundle.getBundle(LiftoffApp.MESSAGES_BUNDLE, Locale.ENGLISH), url -> { },
                    new ErrorLog(null));
            session.selectMission("farm-watch");
            session.next();
            navigator.showMissionStep();
            assertNull(scene.lookup("#error-screen"), "the builder failed to open");
        });
    }

    private ShipBuilderController builder() {
        return (ShipBuilderController) navigator.current();
    }

    private <T extends Node> T find(String selector) {
        scene.getRoot().applyCss();
        scene.getRoot().layout();
        @SuppressWarnings("unchecked")
        T node = (T) scene.lookup(selector);
        assertNotNull(node, "nothing matches " + selector);
        return node;
    }

    private void tab(ModuleSlot slot) {
        this.<ToggleButton>find("#slot-" + slot.name()).fire();
    }

    private static void press(Node node, KeyCode key) {
        Event.fireEvent(node, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", key, false, false, false, false));
    }

    private void fitTheWholeShip() {
        for (int i = 0; i < FARM_PARTS.size(); i++) {
            tab(ModuleSlot.values()[i]);
            this.<ButtonBase>find("#part-" + FARM_PARTS.get(i)).fire();
        }
    }

    // ---- choosing a part: click and drop give the same result -----------------------------------

    @Test
    void clickingACardFitsThePartAndMarksTheCardInWordsNotOnlyColour() {
        FxTest.run(() -> {
            ButtonBase card = find("#part-eo1-class");
            assertFalse(card.getText().contains("Fitted"));

            card.fire();
            assertEquals("eo1-class", session.build().part(ModuleSlot.AIRFRAME).orElseThrow().id());
        });
        FxTest.settle();
        FxTest.run(() -> {
            ButtonBase card = find("#part-eo1-class");
            assertTrue(card.getStyleClass().contains("chosen"));
            assertTrue(card.getText().contains("✓ Fitted"), card.getText());
            assertTrue(card.getAccessibleText().contains("Fitted"));
            ToggleButton airframeTab = find("#slot-AIRFRAME");
            assertTrue(airframeTab.getText().contains("Fitted"), airframeTab.getText());
            assertNotNull(airframeTab.getGraphic());
        });
    }

    @Test
    void droppingACardOnItsOwnPlaceDoesTheSameAsAClick() {
        FxTest.run(() -> {
            assertTrue(builder().dropPart("landsat7-class", ModuleSlot.AIRFRAME));
            assertEquals("landsat7-class", session.build().part(ModuleSlot.AIRFRAME).orElseThrow().id());
        });
        FxTest.settle();
        FxTest.run(() -> {
            ButtonBase card = find("#part-landsat7-class");
            assertTrue(card.getStyleClass().contains("chosen"));
            assertTrue(card.getText().contains("✓ Fitted"));
        });
    }

    @Test
    void droppingACardAnywhereElseChangesNothing() {
        FxTest.run(() -> {
            this.<ButtonBase>find("#part-eo1-class").fire();
            var before = session.build();

            assertFalse(builder().dropPart("landsat7-class", ModuleSlot.THRUSTERS), "wrong place");
            assertFalse(builder().dropPart("landsat7-class", null), "off the ship");
            assertFalse(builder().dropPart("no-such-part", ModuleSlot.AIRFRAME), "unknown part");
            assertFalse(builder().dropPart(null, ModuleSlot.AIRFRAME), "nothing dragged");
            assertFalse(builder().dropPart("mr-106", ModuleSlot.THRUSTERS), "a card from a tab that is not open");

            assertEquals(before, session.build());
        });
    }

    @Test
    void swappingOnePartLeavesTheOtherFourAsTheyWere() {
        FxTest.run(() -> {
            fitTheWholeShip();
            var before = session.build();
            tab(ModuleSlot.POWER);

            this.<ButtonBase>find("#part-shuttle-fuel-cell").fire();

            for (ModuleSlot slot : ModuleSlot.values()) {
                if (slot != ModuleSlot.POWER) {
                    assertEquals(before.part(slot), session.build().part(slot), slot.name());
                }
            }
            assertEquals("shuttle-fuel-cell", session.build().part(ModuleSlot.POWER).orElseThrow().id());
        });
    }

    // ---- Confirm Build ------------------------------------------------------------------------

    @Test
    void confirmBuildIsDisabledAndItsTooltipListsEverythingStillWrong() {
        FxTest.run(() -> {
            Button confirm = find("#confirm");
            assertTrue(confirm.isDisabled());
            String reasons = confirm.getTooltip().getText();
            for (String slot : List.of("Airframe", "Thrusters", "Power", "Cargo bay", "Payload")) {
                assertTrue(reasons.contains("Choose a part for " + slot + "."), reasons);
            }

            this.<ButtonBase>find("#part-eo1-class").fire();
        });
        FxTest.settle();
        FxTest.run(() -> {
            Button confirm = find("#confirm");
            String reasons = confirm.getTooltip().getText();
            assertFalse(reasons.contains("Airframe"), reasons);
            assertTrue(reasons.contains("Thrusters"), reasons);

            Node gate = confirm.getParent();
            assertTrue(gate.isFocusTraversable(), "the reasons can be reached by keyboard while the button is off");
            assertTrue(gate.getAccessibleText().contains("Choose a part for Thrusters."));
        });
    }

    @Test
    void confirmBuildStaysOffUntilSuppliesWereVisitedAndSaysSo() {
        FxTest.run(this::fitTheWholeShip);
        FxTest.settle();
        FxTest.run(() -> {
            Button confirm = find("#confirm");
            assertTrue(session.buildReport().canConfirm());
            assertTrue(confirm.isDisabled());
            assertEquals("Visit Supplies before reviewing the build.", confirm.getTooltip().getText());
            assertFalse(this.<Button>find("#supplies").isDisabled());
        });
    }

    @Test
    void theWarningListShowsAnIconAndASentenceForEveryIssue() {
        FxTest.run(() -> {
            find("#confirm");   // lays the screen out, so the scroll pane holds its content
            List<Node> warnings = List.copyOf(scene.getRoot().lookupAll(".warning"));
            assertEquals(5, warnings.size());
            for (Node warning : warnings) {
                var label = (javafx.scene.control.Label) warning;
                assertNotNull(label.getGraphic(), label.getText());
                assertTrue(label.getText().startsWith("Choose a part for "), label.getText());
            }
        });
    }

    @Test
    void everyGaugeStateIsAWordWithASymbol() {
        FxTest.run(() -> {
            fitTheWholeShip();
            List<Node> states = List.copyOf(scene.getRoot().lookupAll(".gauge-state"));
            assertEquals(4, states.size(), "mass, budget, power and fuel");
            for (Node state : states) {
                String text = ((javafx.scene.control.Label) state).getText();
                assertTrue(text.matches("^[✓◐⚠○] \\p{L}.*"), text);
            }
        });
    }

    // ---- keyboard only ------------------------------------------------------------------------

    @Test
    void arrowKeysMoveBetweenTabsAndEnterSelectsACard() {
        FxTest.run(() -> {
            ToggleButton airframe = find("#slot-AIRFRAME");
            assertTrue(airframe.isFocusTraversable());

            press(airframe, KeyCode.DOWN);
            assertTrue(this.<ToggleButton>find("#slot-THRUSTERS").isSelected());
            assertNotNull(scene.lookup("#part-mr-106"), "the strip follows the tab");

            press(find("#slot-THRUSTERS"), KeyCode.UP);
            assertTrue(airframe.isSelected());
            press(airframe, KeyCode.UP);
            assertTrue(this.<ToggleButton>find("#slot-SATELLITE").isSelected(), "the tabs wrap around");

            ButtonBase card = find("#part-cubesat-3u");
            assertTrue(card.isFocusTraversable());
            press(card, KeyCode.ENTER);
            assertEquals("cubesat-3u", session.build().part(ModuleSlot.SATELLITE).orElseThrow().id());
        });
    }

    @Test
    void arrowKeysMoveFocusAlongTheCardsWithoutFittingAnything() {
        FxTest.run(() -> {
            // No window is shown, so the scene's focus owner is checked; a node only reports
            // isFocused() inside a focused window.
            ButtonBase first = find("#part-sorce-class");
            first.requestFocus();
            assertEquals(first, scene.getFocusOwner());

            press(first, KeyCode.RIGHT);
            assertEquals(find("#part-eo1-class"), scene.getFocusOwner());
            press(find("#part-eo1-class"), KeyCode.LEFT);
            press(first, KeyCode.LEFT);
            assertEquals(find("#part-shuttle-orbiter"), scene.getFocusOwner(), "the cards wrap around");

            assertTrue(session.build().part(ModuleSlot.AIRFRAME).isEmpty(), "moving focus does not fit a part");
        });
    }

    @Test
    void everyControlOnTheScreenCanBeReachedByKeyboard() {
        FxTest.run(() -> {
            for (ModuleSlot slot : ModuleSlot.values()) {
                assertTrue(find("#slot-" + slot.name()).isFocusTraversable(), slot.name());
            }
            assertTrue(this.<CheckBox>find("#calmMode").isFocusTraversable());
            scene.getRoot().lookupAll(".part-card").forEach(card -> assertTrue(card.isFocusTraversable()));
            scene.getRoot().lookupAll(".strip-arrow").forEach(arrow ->
                    assertNotNull(((Button) arrow).getAccessibleText()));
        });
    }

    // ---- the 3D ship: bay doors, calm mode, camera ----------------------------------------------

    private HangarShipView hangar() {
        assumeTrue(builder().shipView() instanceof HangarShipView,
                "JavaFX 3D is not available on this machine, so the 3D checks did not run");
        return (HangarShipView) builder().shipView();
    }

    @Test
    void theCargoBayDoorsAreOpenOnlyOnThePayloadTab() {
        FxTest.run(() -> {
            HangarShipView hangar = hangar();
            fitTheWholeShip();
            for (ModuleSlot slot : ModuleSlot.values()) {
                tab(slot);
                assertEquals(slot == ModuleSlot.SATELLITE, hangar.isCargoBayOpen(), slot.name());
            }
            tab(ModuleSlot.SATELLITE);
            assertTrue(hangar.isCargoBayOpen());
            tab(ModuleSlot.AIRFRAME);
            assertFalse(hangar.isCargoBayOpen(), "leaving the tab closes the doors");
        });
    }

    @Test
    void swappingAPartNeverMovesTheCamera() {
        FxTest.run(() -> {
            HangarShipView hangar = hangar();
            hangar.orbitCamera(140, -35, 300);
            double[] pose = hangar.cameraPose();
            assertFalse(Arrays.equals(pose, new HangarShipView().cameraPose()), "the camera was moved off its start");

            fitTheWholeShip();
            tab(ModuleSlot.AIRFRAME);
            this.<ButtonBase>find("#part-shuttle-orbiter").fire();
            this.<ButtonBase>find("#part-sorce-class").fire();
            tab(ModuleSlot.SATELLITE);
            this.<ButtonBase>find("#part-cubesat-12u").fire();

            assertArrayEquals(pose, hangar.cameraPose(), 0.0);
        });
    }

    @Test
    void calmModeTurnsTheAnimationsOffAndTheCheckBoxSaysWhatItIs() {
        FxTest.run(() -> {
            HangarShipView hangar = hangar();
            CheckBox calm = find("#calmMode");
            assertEquals("Calm mode", calm.getText());
            assertFalse(hangar.isCalmMode());

            calm.fire();
            assertTrue(hangar.isCalmMode());
            fitTheWholeShip();
            assertFalse(hangar.isAnimating(), "nothing may be moving in calm mode");

            calm.fire();
            assertFalse(hangar.isCalmMode());
        });
    }

    // ---- nothing the player can press may throw -------------------------------------------------

    @Test
    void pressingContinueOnALockedStepKeepsThePlayerOnTheShipInsteadOfFailing() {
        FxTest.run(() -> {
            builder().onNext();

            assertEquals(MissionStep.SHIP, session.step());
            assertNull(scene.lookup("#error-screen"));
            assertNotNull(scene.lookup("#slot-AIRFRAME"));
        });
    }

    @Test
    void everyScreenOfAFullMissionOpensWithoutAnError() {
        FxTest.run(() -> {
            navigator.show(Screen.TITLE);
            navigator.show(Screen.MISSION_SELECT);
            navigator.show(Screen.SOURCES);
            session.selectMission("farm-watch");
            navigator.showMissionStep();
            session.next();
            FARM_PARTS.forEach(session::choosePart);
            navigator.showMissionStep();
            session.next();
            session.addInstrument("ali");
            session.chooseOrbit("sso-705");
            session.toggleStation("svalbard");
            session.chooseLaunchVehicle("atlas-v-401");
            navigator.showMissionStep();
            session.next();
            navigator.showMissionStep();
            session.confirmBuild();
            session.next();
            navigator.showMissionStep();
            session.runLaunchCheck();
            session.next();
            navigator.showMissionStep();
            assertNull(scene.lookup("#error-screen"));
        });
        FxTest.await(() -> session.simulation().isPresent(), "the Fly screen to fly the mission");
        FxTest.run(() -> {
            session.next();
            navigator.showMissionStep();

            assertEquals(MissionStep.REPORT, session.step());
            assertNull(scene.lookup("#error-screen"));
            this.<Button>find(".primary").fire();
            assertEquals(MissionStep.SHIP, session.step());
            assertTrue(session.build().isComplete(), "Redesign keeps the parts");
        });
    }
}
