package com.liftoff.minigame;

import com.liftoff.minigame.MatchBoard.Cell;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchBoardTest {


    private static int[][] columns(int[]... columns) {
        return columns;
    }

    @Test
    void aNewBoardHasNoMatchesButAlwaysAMove() {
        for (long seed = 0; seed < 40; seed++) {
            var board = new MatchBoard(8, 7, 6, seed);
            assertTrue(MatchBoard.matches(board.snapshot()).isEmpty(), "seed " + seed);
            assertTrue(board.findMove().isPresent(), "seed " + seed);
        }
    }

    @Test
    void findsHorizontalAndVerticalLinesOfThreeOrMore() {
        var grid = columns(
                new int[] {0, 1, 2},
                new int[] {0, 1, 3},
                new int[] {0, 1, 4},
                new int[] {5, 1, 4});
        assertEquals(Set.of(new Cell(0, 0), new Cell(1, 0), new Cell(2, 0),
                new Cell(0, 1), new Cell(1, 1), new Cell(2, 1), new Cell(3, 1)), MatchBoard.matches(grid));
    }

    @Test
    void aSwapThatMakesNoMatchIsRefusedAndChangesNothing() {
        var board = new MatchBoard(columns(
                new int[] {0, 1, 2},
                new int[] {1, 2, 0},
                new int[] {2, 0, 1}), 3, 1);
        var before = board.snapshot();
        assertFalse(board.wouldMatch(new Cell(0, 0), new Cell(0, 1)));
        assertTrue(board.swap(new Cell(0, 0), new Cell(0, 1)).isEmpty());
        assertArrayEquals(before, board.snapshot());
        assertTrue(board.swap(new Cell(0, 0), new Cell(2, 2)).isEmpty(), "only neighbours swap");
    }

    @Test
    void matchedTilesClearAndTheTilesAboveFallDown() {

        var board = new MatchBoard(columns(
                new int[] {3, 4, 0},
                new int[] {4, 0, 1},
                new int[] {3, 4, 0},
                new int[] {4, 3, 0}), 5, 9);
        var cascades = board.swap(new Cell(1, 1), new Cell(1, 2));
        assertFalse(cascades.isEmpty());
        var first = cascades.get(0);
        assertEquals(Set.of(new Cell(0, 2), new Cell(1, 2), new Cell(2, 2), new Cell(3, 2)), first.cleared());

        assertEquals(1, first.fallFrom()[0][2]);
        assertEquals(0, first.fallFrom()[0][1]);
        assertEquals(-1, first.fallFrom()[0][0]);
        assertEquals(4, first.after()[0][2]);
        assertEquals(3, first.after()[0][1]);

        assertEquals(1, first.after()[1][2]);
        assertEquals(1, first.fallFrom()[1][2]);
    }

    @Test
    void theGameCountsCollectedTilesAndMoves() {
        var game = new StarMatchGame(new MatchBoard(columns(
                new int[] {3, 4, 0},
                new int[] {4, 0, 1},
                new int[] {3, 4, 0},
                new int[] {4, 3, 0}), 5, 9), Challenge.MEDIUM);
        assertTrue(game.play(new Cell(0, 0), new Cell(3, 3)).isEmpty());
        assertEquals(Challenge.MEDIUM.matchMoves(), game.movesLeft(), "a refused swap costs no move");
        var cascades = game.play(new Cell(1, 1), new Cell(1, 2));
        int cleared = cascades.stream().mapToInt(cascade -> cascade.cleared().size()).sum();
        assertEquals(cleared, game.collected());
        assertTrue(game.movesLeft() >= Challenge.MEDIUM.matchMoves() - 1);
        assertFalse(game.won());
    }
}
