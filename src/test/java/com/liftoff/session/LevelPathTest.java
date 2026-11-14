package com.liftoff.session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LevelPathTest {

    private final EnumSet<MissionStep> unlocked = EnumSet.of(MissionStep.BRIEFING);
    private MissionStep current = MissionStep.BRIEFING;
    private LevelPath path;

    @BeforeEach
    void setUp() {
        path = new LevelPath(unlocked::contains, () -> current);
    }

    @Test
    void fiveLevelsWithTheMiniGamesSecondAndFourth() {
        assertEquals(5, Level.values().length);
        assertEquals(Level.Kind.MINI_GAME, Level.STAR_MATCH.kind());
        assertEquals(2, Level.STAR_MATCH.number());
        assertEquals(Level.Kind.MINI_GAME, Level.ASTEROID_DASH.kind());
        assertEquals(4, Level.ASTEROID_DASH.number());
        assertEquals(Level.BUILD, Level.of(MissionStep.BRIEFING));
        assertEquals(Level.BUILD, Level.of(MissionStep.SHIP));
        assertEquals(Level.PACK, Level.of(MissionStep.SUPPLIES));
        assertEquals(Level.LIFTOFF, Level.of(MissionStep.REPORT));
    }

    @Test
    void aLevelOpensOnlyWhenEveryLevelBeforeItIsDone() {
        assertTrue(path.isOpen(Level.BUILD));
        assertFalse(path.isOpen(Level.STAR_MATCH));

        unlocked.add(MissionStep.SHIP);
        assertFalse(path.isDone(Level.BUILD), "reading the briefing is only the start of level 1");
        unlocked.add(MissionStep.SUPPLIES);
        assertTrue(path.isDone(Level.BUILD));
        assertTrue(path.isOpen(Level.STAR_MATCH));
        assertFalse(path.isOpen(Level.PACK), "packing waits for the first mini-game");

        path.finishGame(Level.STAR_MATCH);
        assertTrue(path.isOpen(Level.PACK));
        assertEquals(Level.PACK, path.toPlay());
    }

    @Test
    void aMiniGameCannotBeFinishedBeforeItOpens() {
        assertThrows(IllegalStateException.class, () -> path.finishGame(Level.STAR_MATCH));
        assertThrows(IllegalArgumentException.class, () -> path.finishGame(Level.PACK));
    }

    @Test
    void changingTheShipLocksTheLevelsAfterPackingAgain() {
        unlocked.addAll(List.of(MissionStep.SHIP, MissionStep.SUPPLIES, MissionStep.REVIEW, MissionStep.LAUNCH));
        path.finishGame(Level.STAR_MATCH);
        path.finishGame(Level.ASTEROID_DASH);
        assertTrue(path.isOpen(Level.LIFTOFF));

        unlocked.remove(MissionStep.LAUNCH);
        assertFalse(path.isDone(Level.PACK));
        assertFalse(path.isOpen(Level.LIFTOFF));
        assertTrue(path.isDone(Level.ASTEROID_DASH), "a finished game stays finished");
    }

    @Test
    void theShipTravelsToTheNextLevelThatIsNotDone() {
        unlocked.addAll(List.of(MissionStep.SHIP, MissionStep.SUPPLIES));
        assertEquals(List.of(Level.STAR_MATCH), path.routeAfter(Level.BUILD));

        unlocked.addAll(List.of(MissionStep.REVIEW, MissionStep.LAUNCH));
        path.finishGame(Level.STAR_MATCH);
        path.finishGame(Level.ASTEROID_DASH);
        assertEquals(List.of(Level.ASTEROID_DASH, Level.LIFTOFF), path.routeAfter(Level.PACK),
                "a finished mini-game is flown past, not replayed");
    }

    @Test
    void replayingAnOldLevelWithEverythingAfterItDoneHasNoTravel() {
        unlocked.addAll(EnumSet.allOf(MissionStep.class));
        path.finishGame(Level.STAR_MATCH);
        path.finishGame(Level.ASTEROID_DASH);
        assertTrue(path.routeAfter(Level.BUILD).isEmpty());
        assertTrue(path.allDone());
    }

    @Test
    void enteringALevelResumesItsCurrentStepOrStartsAtItsFirst() {
        unlocked.addAll(List.of(MissionStep.SHIP, MissionStep.SUPPLIES, MissionStep.REVIEW));
        path.finishGame(Level.STAR_MATCH);
        current = MissionStep.REVIEW;
        assertEquals(MissionStep.REVIEW, path.entryStep(Level.PACK));
        assertEquals(MissionStep.BRIEFING, path.entryStep(Level.BUILD));
        assertThrows(IllegalArgumentException.class, () -> path.entryStep(Level.STAR_MATCH));
    }

    @Test
    void aFinishedFlightReopensOnItsReport() {
        unlocked.addAll(EnumSet.allOf(MissionStep.class));
        current = MissionStep.SHIP;
        assertEquals(MissionStep.REPORT, path.entryStep(Level.LIFTOFF));
        unlocked.remove(MissionStep.REPORT);
        assertEquals(MissionStep.LAUNCH, path.entryStep(Level.LIFTOFF));
    }

    @Test
    void moveToRecordsWhereTheShipIsAndRefusesLockedLevels() {
        assertEquals(Level.BUILD, path.position());
        assertThrows(IllegalStateException.class, () -> path.moveTo(Level.PACK));
        unlocked.addAll(List.of(MissionStep.SHIP, MissionStep.SUPPLIES));
        path.moveTo(Level.STAR_MATCH);
        assertEquals(Level.STAR_MATCH, path.position());
        path.reset();
        assertEquals(Level.BUILD, path.position());
        assertFalse(path.isDone(Level.STAR_MATCH));
    }
}
