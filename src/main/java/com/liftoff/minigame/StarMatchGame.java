package com.liftoff.minigame;

import java.util.List;

/**
 * Star Match, the first mini-game: collect enough space tiles before the moves run out. A swap
 * that clears five or more tiles in one go earns a bonus move. Just for fun: nothing here
 * touches the mission design or its outcome.
 */
public final class StarMatchGame {

    public static final int COLUMNS = 8;
    public static final int ROWS = 7;
    public static final int BONUS_AT = 5;

    private final MatchBoard board;
    private final Challenge challenge;
    private int movesLeft;
    private int collected;

    public StarMatchGame(long seed, Challenge challenge) {
        this(new MatchBoard(COLUMNS, ROWS, challenge.matchKinds(), seed), challenge);
    }

    StarMatchGame(MatchBoard board, Challenge challenge) {
        this.board = board;
        this.challenge = challenge;
        this.movesLeft = challenge.matchMoves();
    }

    public Challenge challenge() {
        return challenge;
    }

    public int goal() {
        return challenge.matchGoal();
    }

    public MatchBoard board() {
        return board;
    }

    public int movesLeft() {
        return movesLeft;
    }

    public int collected() {
        return collected;
    }

    public boolean won() {
        return collected >= goal();
    }

    public boolean outOfMoves() {
        return !won() && movesLeft == 0;
    }

    public boolean over() {
        return won() || movesLeft == 0;
    }

    /** Plays a swap. Returns no cascades (and costs no move) when the swap makes no match or the game is over. */
    public List<MatchBoard.Cascade> play(MatchBoard.Cell a, MatchBoard.Cell b) {
        if (over()) {
            return List.of();
        }
        var cascades = board.swap(a, b);
        if (cascades.isEmpty()) {
            return cascades;
        }
        movesLeft--;
        for (var cascade : cascades) {
            collected += cascade.cleared().size();
            if (cascade.cleared().size() >= BONUS_AT) {
                movesLeft++;
            }
        }
        return cascades;
    }
}
