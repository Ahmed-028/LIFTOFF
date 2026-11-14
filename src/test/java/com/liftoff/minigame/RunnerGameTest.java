package com.liftoff.minigame;

import com.liftoff.minigame.RunnerGame.Kind;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunnerGameTest {

    private static RunnerGame emptyTrack() {
        var game = new RunnerGame(1, false);
        game.clearTrack();
        return game;
    }

    private static void run(RunnerGame game, double seconds) {
        for (double t = 0; t < seconds; t += .02) {
            game.update(.02);
        }
    }

    @Test
    void hittingARockCostsAShieldAndGivesAMomentOfSafety() {
        var game = emptyTrack();
        game.place(Kind.ROCK, 0, 4);
        game.place(Kind.ROCK, 0, 6);
        run(game, .6);
        assertEquals(RunnerGame.SHIELDS - 1, game.shields(), "the second rock arrives while still safe");
        assertEquals(1, game.hits());
    }

    @Test
    void jumpingClearsALowRock() {
        var game = emptyTrack();
        game.place(Kind.ROCK, 0, 5);
        run(game, .1);
        game.jump();
        run(game, 1);
        assertEquals(RunnerGame.SHIELDS, game.shields());
    }

    @Test
    void duckingPassesUnderABeamButJumpingDoesNot() {
        var ducked = emptyTrack();
        ducked.place(Kind.BEAM, 0, 4);
        ducked.duck();
        run(ducked, .6);
        assertEquals(RunnerGame.SHIELDS, ducked.shields());

        var jumped = emptyTrack();
        jumped.place(Kind.BEAM, 0, 6);
        run(jumped, .1);
        jumped.jump();
        run(jumped, .8);
        assertEquals(RunnerGame.SHIELDS - 1, jumped.shields());
    }

    @Test
    void changingLaneDodgesABoulderAndCollectsStars() {
        var game = emptyTrack();
        game.place(Kind.BOULDER, 0, 8);
        game.place(Kind.STAR, 1, 8);
        game.right();
        run(game, 1);
        assertEquals(1, game.currentLane());
        assertEquals(RunnerGame.SHIELDS, game.shields());
        assertEquals(1, game.stars());
    }

    @Test
    void harderMissionsMeanALongerFasterRide() {
        var easy = new RunnerGame(3, Challenge.EASY, false);
        var hard = new RunnerGame(3, Challenge.HARD, false);
        assertTrue(hard.finish() > easy.finish());
        assertTrue(hard.speed() > easy.speed());
        var calm = new RunnerGame(3, Challenge.HARD, true);
        assertTrue(calm.speed() < hard.speed(), "calm mode slows the ride down");
        assertTrue(Challenge.HARD.matchGoal() > Challenge.EASY.matchGoal());
        assertTrue(Challenge.HARD.matchMoves() < Challenge.EASY.matchMoves());
        assertEquals(Challenge.EASY, Challenge.forTier(0));
        assertEquals(Challenge.HARD, Challenge.forTier(2));
    }

    @Test
    void theRiderStaysOnTheThreeLanes() {
        var game = emptyTrack();
        game.left();
        game.left();
        game.left();
        assertEquals(-1, game.lane());
        game.right();
        game.right();
        game.right();
        assertEquals(1, game.lane());
    }

    @Test
    void everyRowOfObstaclesLeavesALaneClear() {
        for (long seed = 0; seed < 30; seed++) {
            var game = new RunnerGame(seed, false);
            Map<Double, Set<Integer>> blocked = new HashMap<>();
            for (var thing : game.things()) {
                if (thing.kind() != Kind.STAR) {
                    blocked.computeIfAbsent(thing.z(), z -> new HashSet<>()).add(thing.lane());
                }
            }
            blocked.values().forEach(lanes -> assertTrue(lanes.size() < 3, "a row blocks every lane"));
        }
    }

    @Test
    void theRunEndsAtTheFinishOrWhenTheShieldsRunOut() {
        var game = emptyTrack();
        run(game, 200);
        assertTrue(game.finished());
        assertTrue(game.over());

        var crashed = emptyTrack();
        for (int i = 0; i < RunnerGame.SHIELDS; i++) {
            crashed.place(Kind.BOULDER, 0, 5 + i * 30);
        }
        run(crashed, 8);
        assertTrue(crashed.crashed());
        assertFalse(crashed.finished());
        double stoppedAt = crashed.distance();
        crashed.update(1);
        assertEquals(stoppedAt, crashed.distance(), "nothing moves after the run is over");
    }
}
