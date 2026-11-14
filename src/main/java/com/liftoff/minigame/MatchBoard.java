package com.liftoff.minigame;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * The grid of the Star Match puzzle. Swapping two neighbouring tiles is allowed only when it
 * lines up three or more of a kind; matched tiles clear, the tiles above fall down and new ones
 * drop in from the top, which can set off more matches (a cascade).
 */
public final class MatchBoard {

    /** A grid position; row 0 is the top. */
    public record Cell(int col, int row) {
        public boolean nextTo(Cell other) {
            return Math.abs(col - other.col) + Math.abs(row - other.row) == 1;
        }
    }

    public record Move(Cell from, Cell to) {
    }

    /**
     * One round of clearing and falling.
     *
     * @param cleared  the matched cells, before falling
     * @param after    the kinds after the fall, {@code after[col][row]}
     * @param fallFrom the row each tile in {@code after} fell from; negative rows are new tiles
     *                 dropping in from above the board
     */
    public record Cascade(Set<Cell> cleared, int[][] after, int[][] fallFrom) {
    }

    private final int cols;
    private final int rows;
    private final int kinds;
    private final int[][] grid;
    private final SplittableRandom random;
    private boolean reshuffled;

    public MatchBoard(int cols, int rows, int kinds, long seed) {
        if (cols < 3 || rows < 3 || kinds < 3) {
            throw new IllegalArgumentException("A match board needs at least 3 columns, 3 rows and 3 kinds");
        }
        this.cols = cols;
        this.rows = rows;
        this.kinds = kinds;
        this.grid = new int[cols][rows];
        this.random = new SplittableRandom(seed);
        fillWithoutMatches();
    }

    /** A board with the given tiles, for tests. New tiles still come from the seeded random. */
    MatchBoard(int[][] tiles, int kinds, long seed) {
        this.cols = tiles.length;
        this.rows = tiles[0].length;
        this.kinds = kinds;
        this.grid = new int[cols][rows];
        for (int c = 0; c < cols; c++) {
            System.arraycopy(tiles[c], 0, grid[c], 0, rows);
        }
        this.random = new SplittableRandom(seed);
    }

    public int cols() {
        return cols;
    }

    public int rows() {
        return rows;
    }

    public int kinds() {
        return kinds;
    }

    public int kind(Cell cell) {
        return grid[cell.col()][cell.row()];
    }

    public int[][] snapshot() {
        var copy = new int[cols][];
        for (int c = 0; c < cols; c++) {
            copy[c] = grid[c].clone();
        }
        return copy;
    }

    /** True if the last swap left no possible move, so the board was shuffled. */
    public boolean reshuffled() {
        return reshuffled;
    }

    public boolean inside(Cell cell) {
        return cell.col() >= 0 && cell.col() < cols && cell.row() >= 0 && cell.row() < rows;
    }

    /** Whether swapping the two cells would line up three or more. */
    public boolean wouldMatch(Cell a, Cell b) {
        if (!inside(a) || !inside(b) || !a.nextTo(b)) {
            return false;
        }
        swapTiles(a, b);
        boolean match = !matches(grid).isEmpty();
        swapTiles(a, b);
        return match;
    }

    /**
     * Swaps two neighbouring tiles and resolves every cascade. Returns no cascades and leaves the
     * board unchanged if the swap would not make a match.
     */
    public List<Cascade> swap(Cell a, Cell b) {
        reshuffled = false;
        if (!wouldMatch(a, b)) {
            return List.of();
        }
        swapTiles(a, b);
        var cascades = new ArrayList<Cascade>();
        for (Set<Cell> matched = matches(grid); !matched.isEmpty(); matched = matches(grid)) {
            cascades.add(collapse(matched));
        }
        if (findMove().isEmpty()) {
            fillWithoutMatches();
            reshuffled = true;
        }
        return cascades;
    }

    /** Any swap that makes a match, scanning from the top left. */
    public Optional<Move> findMove() {
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                var here = new Cell(c, r);
                for (var there : new Cell[] {new Cell(c + 1, r), new Cell(c, r + 1)}) {
                    if (wouldMatch(here, there)) {
                        return Optional.of(new Move(here, there));
                    }
                }
            }
        }
        return Optional.empty();
    }

    /** Every cell that is part of a line of three or more of the same kind. */
    static Set<Cell> matches(int[][] grid) {
        int cols = grid.length;
        int rows = grid[0].length;
        var found = new LinkedHashSet<Cell>();
        for (int r = 0; r < rows; r++) {
            int start = 0;
            for (int c = 1; c <= cols; c++) {
                if (c == cols || grid[c][r] != grid[start][r] || grid[start][r] < 0) {
                    if (c - start >= 3 && grid[start][r] >= 0) {
                        for (int k = start; k < c; k++) {
                            found.add(new Cell(k, r));
                        }
                    }
                    start = c;
                }
            }
        }
        for (int c = 0; c < cols; c++) {
            int start = 0;
            for (int r = 1; r <= rows; r++) {
                if (r == rows || grid[c][r] != grid[c][start] || grid[c][start] < 0) {
                    if (r - start >= 3 && grid[c][start] >= 0) {
                        for (int k = start; k < r; k++) {
                            found.add(new Cell(c, k));
                        }
                    }
                    start = r;
                }
            }
        }
        return found;
    }

    private Cascade collapse(Set<Cell> matched) {
        for (Cell cell : matched) {
            grid[cell.col()][cell.row()] = -1;
        }
        var fallFrom = new int[cols][rows];
        for (int c = 0; c < cols; c++) {
            int write = rows - 1;
            for (int r = rows - 1; r >= 0; r--) {
                if (grid[c][r] >= 0) {
                    grid[c][write] = grid[c][r];
                    fallFrom[c][write] = r;
                    write--;
                }
            }
            int empty = write + 1;
            for (int r = write; r >= 0; r--) {
                grid[c][r] = random.nextInt(kinds);
                fallFrom[c][r] = r - empty;
            }
        }
        return new Cascade(Set.copyOf(matched), snapshot(), fallFrom);
    }

    private void fillWithoutMatches() {
        do {
            for (int c = 0; c < cols; c++) {
                for (int r = 0; r < rows; r++) {
                    int kind;
                    do {
                        kind = random.nextInt(kinds);
                    } while ((c >= 2 && grid[c - 1][r] == kind && grid[c - 2][r] == kind)
                            || (r >= 2 && grid[c][r - 1] == kind && grid[c][r - 2] == kind));
                    grid[c][r] = kind;
                }
            }
        } while (findMove().isEmpty());
    }

    private void swapTiles(Cell a, Cell b) {
        int kind = grid[a.col()][a.row()];
        grid[a.col()][a.row()] = grid[b.col()][b.row()];
        grid[b.col()][b.row()] = kind;
    }
}
