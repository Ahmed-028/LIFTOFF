package com.liftoff.session;

import com.liftoff.model.ModuleSlot;
import com.liftoff.model.ShipBuild;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.liftoff.session.MissionStep.BRIEFING;
import static com.liftoff.session.MissionStep.FLY;
import static com.liftoff.session.MissionStep.LAUNCH;
import static com.liftoff.session.MissionStep.REPORT;
import static com.liftoff.session.MissionStep.REVIEW;
import static com.liftoff.session.MissionStep.SHIP;
import static com.liftoff.session.MissionStep.SUPPLIES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionFlowTest {

    private ShipBuild build;
    private MissionFlow flow;

    @BeforeEach
    void newFlow() {
        build = ShipBuild.empty();
        flow = new MissionFlow(() -> build.isComplete());
    }

    private void completeShip() {
        build = FakeParts.completeBuild();
        flow.partsChanged();
    }

    /** Briefing read, ship complete, supplies visited, standing on REVIEW. */
    private void reachReview() {
        flow.next();
        completeShip();
        flow.next();
        flow.next();
        assertEquals(REVIEW, flow.current());
    }

    private void reachReport() {
        reachReview();
        flow.confirmBuild();
        flow.next();
        flow.markLaunchPassed();
        flow.next();
        flow.markSimulationFinished();
        flow.next();
        assertEquals(REPORT, flow.current());
    }

    @Test
    void aNewFlowStartsAtTheBriefingWithEverythingElseLocked() {
        assertEquals(BRIEFING, flow.current());
        assertTrue(flow.isUnlocked(BRIEFING));
        assertEquals(Optional.empty(), flow.reasonLocked(BRIEFING));
        for (MissionStep step : MissionStep.values()) {
            if (step != BRIEFING) {
                assertFalse(flow.isUnlocked(step), step.name());
            }
        }
        assertEquals(Optional.of("flow.locked.briefingUnread"), flow.reasonLocked(SHIP));
    }

    @Test
    void theShipStepCannotBeJumpedToBeforeTheBriefingIsRead() {
        IllegalStateException refused = assertThrows(IllegalStateException.class, () -> flow.goTo(SHIP));

        assertEquals("flow.locked.briefingUnread", refused.getMessage());
        assertEquals(BRIEFING, flow.current());
    }

    @Test
    void nextFromTheBriefingMarksItReadAndOpensTheShipStep() {
        assertEquals(SHIP, flow.next());
        assertTrue(flow.isUnlocked(SHIP));
    }

    @Test
    void suppliesCannotBeReachedWithAnIncompleteShip() {
        flow.next();
        build = build.with(FakeParts.part("airframe-a", ModuleSlot.AIRFRAME));
        flow.partsChanged();

        assertFalse(flow.isUnlocked(SUPPLIES));
        assertEquals(Optional.of("flow.locked.shipIncomplete"), flow.reasonLocked(SUPPLIES));
        assertThrows(IllegalStateException.class, flow::next);
        assertThrows(IllegalStateException.class, () -> flow.goTo(SUPPLIES));
        assertEquals(SHIP, flow.current());
    }

    @Test
    void aCompleteShipOpensSupplies() {
        flow.next();
        completeShip();

        assertEquals(SUPPLIES, flow.next());
    }

    @Test
    void reviewNeedsTheSuppliesStepVisited() {
        flow.next();
        completeShip();

        assertEquals(Optional.of("flow.locked.suppliesNotVisited"), flow.reasonLocked(REVIEW));
        assertThrows(IllegalStateException.class, () -> flow.goTo(REVIEW));

        flow.goTo(SUPPLIES);
        assertTrue(flow.isUnlocked(REVIEW));
    }

    @Test
    void launchIsLockedUntilTheBuildIsConfirmed() {
        reachReview();

        assertEquals(Optional.of("flow.locked.buildNotConfirmed"), flow.reasonLocked(LAUNCH));
        assertThrows(IllegalStateException.class, flow::next);
        assertEquals(REVIEW, flow.current());

        flow.confirmBuild();
        assertEquals(LAUNCH, flow.next());
    }

    @Test
    void anIncompleteShipCannotBeConfirmed() {
        flow.next();

        assertThrows(IllegalStateException.class, flow::confirmBuild);
    }

    @Test
    void flyNeedsThePassedLaunchCheckAndReportNeedsAFinishedSimulation() {
        reachReview();
        flow.confirmBuild();
        flow.next();

        assertEquals(Optional.of("flow.locked.launchNotPassed"), flow.reasonLocked(FLY));
        assertThrows(IllegalStateException.class, flow::next);
        flow.markLaunchPassed();
        assertEquals(FLY, flow.next());

        assertEquals(Optional.of("flow.locked.simulationNotFinished"), flow.reasonLocked(REPORT));
        assertThrows(IllegalStateException.class, flow::next);
        flow.markSimulationFinished();
        assertEquals(REPORT, flow.next());
    }

    @Test
    void theLaunchCheckCannotPassOnAnUnconfirmedBuild() {
        reachReview();

        assertThrows(IllegalStateException.class, flow::markLaunchPassed);
        assertThrows(IllegalStateException.class, flow::markSimulationFinished);
    }

    @Test
    void changingAPartAfterConfirmingLocksLaunchAgain() {
        reachReview();
        flow.confirmBuild();
        assertTrue(flow.isUnlocked(LAUNCH));

        flow.goTo(SHIP);
        build = build.with(FakeParts.part("power-b", ModuleSlot.POWER));
        flow.partsChanged();

        assertFalse(flow.isUnlocked(LAUNCH));
        assertFalse(flow.isUnlocked(FLY));
        assertFalse(flow.isUnlocked(REPORT));
        assertEquals(Optional.of("flow.locked.buildNotConfirmed"), flow.reasonLocked(LAUNCH));
        assertTrue(flow.isUnlocked(REVIEW));
        assertEquals(SHIP, flow.current());
    }

    @Test
    void goingBackIsAlwaysAllowedAndKeepsWhatWasUnlocked() {
        reachReview();
        flow.confirmBuild();
        flow.next();

        assertEquals(REVIEW, flow.back());
        assertEquals(BRIEFING, flow.goTo(BRIEFING));
        assertTrue(flow.isUnlocked(LAUNCH));
        assertEquals(LAUNCH, flow.goTo(LAUNCH));
    }

    @Test
    void backAtTheBriefingAndNextAtTheReportStayPut() {
        assertEquals(BRIEFING, flow.back());
        assertEquals(BRIEFING, flow.current());

        reachReport();
        assertEquals(REPORT, flow.next());
        assertEquals(REPORT, flow.current());
        assertEquals(FLY, flow.back());
    }

    @Test
    void redesignReturnsToTheShipStepWithThePartsKeptAndTheResultsCleared() {
        reachReport();
        ShipBuild before = build;

        flow.redesign();

        assertEquals(SHIP, flow.current());
        assertEquals(before, build);
        assertTrue(flow.isUnlocked(SUPPLIES));
        assertTrue(flow.isUnlocked(REVIEW));
        assertFalse(flow.isUnlocked(LAUNCH));
        assertFalse(flow.isUnlocked(FLY));
        assertFalse(flow.isUnlocked(REPORT));
    }

    @Test
    void redesignIsOnlyOfferedFromTheReport() {
        reachReview();

        assertThrows(IllegalStateException.class, flow::redesign);
    }
}
