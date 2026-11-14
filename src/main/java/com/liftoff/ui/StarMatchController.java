package com.liftoff.ui;

import com.liftoff.minigame.MatchBoard;
import com.liftoff.minigame.MatchBoard.Cell;
import com.liftoff.minigame.StarMatchGame;
import com.liftoff.render.SpaceTiles;
import com.liftoff.render.Theme;
import com.liftoff.session.Level;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.List;

/** Level 2: Star Match, a swap-three-in-a-row puzzle with space tiles. */
public final class StarMatchController extends MiniGameScreen {

    private static final double TILE = 66;

    @FXML private StackPane boardHost;
    @FXML private Label goal;
    @FXML private ProgressBar goalProgress;
    @FXML private Label collected;
    @FXML private Label moves;
    @FXML private Label status;
    @FXML private Button finish;
    @FXML private Label difficulty;
    @FXML private Label subtitle;
    private java.util.List<String> tileSet;

    private final Pane board = new Pane();
    private final Rectangle cursor = new Rectangle(TILE - 2, TILE - 2);
    private StarMatchGame game;
    private Node[][] tiles;
    private Cell focus = new Cell(0, 0);
    private Cell picked;
    private Cell pressed;
    private double pressX;
    private double pressY;
    private boolean busy;
    private Node overlay;
    private Animation running;
    private final PauseTransition hint = new PauseTransition(Duration.seconds(7));

    @Override
    protected Level level() {
        return Level.STAR_MATCH;
    }

    @Override
    protected void setUp() {
        // Each mission plays with its own tiles: farms on Earth, landers on the Moon, rovers on Mars.
        var destination = mission().destination();
        tileSet = SpaceTiles.set(destination);
        subtitle.setText(text.get("match.subtitle." + destination.name()));
        board.getStyleClass().add("match-board");
        board.setFocusTraversable(true);
        board.setAccessibleRoleDescription(text.get("match.board"));
        cursor.getStyleClass().add("match-cursor");
        cursor.setArcWidth(20);
        cursor.setArcHeight(20);
        cursor.setMouseTransparent(true);
        board.addEventHandler(MouseEvent.MOUSE_PRESSED, this::onPress);
        board.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::onDrag);
        board.addEventHandler(KeyEvent.KEY_PRESSED, this::onKey);
        board.focusedProperty().addListener((property, was, focused) -> cursor.setVisible(focused));
        boardHost.getChildren().add(new Group(board));
        
        hint.setOnFinished(event -> showHint());
        newGame();
        board.requestFocus();
    }

    private void newGame() {
        game = new StarMatchGame(System.nanoTime(), challenge());
        goal.setText(text.get("match.goal", game.goal(), game.challenge().matchMoves()));
        difficulty.setText(difficultyText());
        var grid = game.board();
        double width = grid.cols() * TILE;
        double height = grid.rows() * TILE;
        board.setPrefSize(width, height);
        board.setMinSize(width, height);
        board.setMaxSize(width, height);
        fit(boardHost, board, width, height, 1.35);
        picked = null;
        busy = false;
        if (overlay != null) {
            boardHost.getChildren().remove(overlay);
            overlay = null;
        }
        finish.setDisable(true);
        drawAll(grid.snapshot(), null);
        status.setText(text.get("match.start"));
        refreshHud();
        hint.playFromStart();
    }

    // ---- input ----

    private void onPress(MouseEvent event) {
        board.requestFocus();
        var cell = cellAt(event.getX(), event.getY());
        if (cell == null) {
            return;
        }
        focus = cell;
        pressed = cell;
        pressX = event.getX();
        pressY = event.getY();
        if (picked != null && picked.nextTo(cell)) {
            pressed = null;
            trySwap(picked, cell);
        } else if (cell.equals(picked)) {
            pick(null);
        } else {
            pick(cell);
        }
        placeCursor();
    }

    private void onDrag(MouseEvent event) {
        if (pressed == null) {
            return;
        }
        double dx = event.getX() - pressX;
        double dy = event.getY() - pressY;
        if (Math.max(Math.abs(dx), Math.abs(dy)) < TILE * .45) {
            return;
        }
        var target = Math.abs(dx) > Math.abs(dy)
                ? new Cell(pressed.col() + (int) Math.signum(dx), pressed.row())
                : new Cell(pressed.col(), pressed.row() + (int) Math.signum(dy));
        var from = pressed;
        pressed = null;
        if (game.board().inside(target)) {
            trySwap(from, target);
        }
    }

    private void onKey(KeyEvent event) {
        int dc = 0;
        int dr = 0;
        switch (event.getCode()) {
            case LEFT, A -> dc = -1;
            case RIGHT, D -> dc = 1;
            case UP, W -> dr = -1;
            case DOWN, S -> dr = 1;
            case SPACE, ENTER -> {
                pick(focus.equals(picked) ? null : focus);
                event.consume();
                return;
            }
            case ESCAPE -> {
                pick(null);
                event.consume();
                return;
            }
            default -> {
                return;
            }
        }
        event.consume();
        var next = new Cell(focus.col() + dc, focus.row() + dr);
        if (!game.board().inside(next)) {
            return;
        }
        if (picked != null) {
            trySwap(picked, next);
        } else {
            focus = next;
            placeCursor();
            describeFocus();
        }
        hint.playFromStart();
    }

    private Cell cellAt(double x, double y) {
        var cell = new Cell((int) Math.floor(x / TILE), (int) Math.floor(y / TILE));
        return game.board().inside(cell) ? cell : null;
    }

    private void pick(Cell cell) {
        if (picked != null && tiles != null) {
            tiles[picked.col()][picked.row()].getStyleClass().remove("picked");
            tiles[picked.col()][picked.row()].setScaleX(1);
            tiles[picked.col()][picked.row()].setScaleY(1);
        }
        picked = busy || game.over() ? null : cell;
        if (picked != null) {
            var node = tiles[picked.col()][picked.row()];
            node.getStyleClass().add("picked");
            node.setScaleX(1.08);
            node.setScaleY(1.08);
            status.setText(text.get("match.picked", tileName(picked)));
        }
        describeFocus();
    }

    // ---- playing ----

    private void trySwap(Cell a, Cell b) {
        if (busy || game.over()) {
            return;
        }
        hint.stop();
        pick(null);
        if (!game.board().wouldMatch(a, b)) {
            shake(tiles[a.col()][a.row()]);
            shake(tiles[b.col()][b.row()]);
            status.setText(text.get("match.noMatch"));
            com.liftoff.audio.Audio.get().play(com.liftoff.audio.Sfx.ERROR);
            hint.playFromStart();
            return;
        }
        busy = true;
        focus = b;
        placeCursor();
        var nodeA = tiles[a.col()][a.row()];
        var nodeB = tiles[b.col()][b.row()];
        var moveA = new TranslateTransition(ms(170), nodeA);
        moveA.setByX((b.col() - a.col()) * TILE);
        moveA.setByY((b.row() - a.row()) * TILE);
        var moveB = new TranslateTransition(ms(170), nodeB);
        moveB.setByX((a.col() - b.col()) * TILE);
        moveB.setByY((a.row() - b.row()) * TILE);
        var swap = new ParallelTransition(moveA, moveB);
        swap.setOnFinished(event -> {
            tiles[a.col()][a.row()] = nodeB;
            tiles[b.col()][b.row()] = nodeA;
            playCascades(game.play(a, b), 0);
        });
        play(swap);
    }

    private void playCascades(List<MatchBoard.Cascade> cascades, int index) {
        if (index == cascades.size()) {
            drawAll(game.board().snapshot(), null);
            busy = false;
            if (game.board().reshuffled()) {
                status.setText(text.get("match.shuffled"));
            }
            refreshHud();
            checkEnd();
            return;
        }
        var cascade = cascades.get(index);
        var vanish = new ParallelTransition();
        for (Cell cell : cascade.cleared()) {
            var node = tiles[cell.col()][cell.row()];
            var shrink = new ScaleTransition(ms(200), node);
            shrink.setToX(0);
            shrink.setToY(0);
            var fade = new FadeTransition(ms(200), node);
            fade.setToValue(0);
            vanish.getChildren().addAll(shrink, fade);
        }
        status.setText(text.get(index == 0 ? "match.cleared" : "match.combo", cascade.cleared().size(), index + 1));
        com.liftoff.audio.Audio.get().play(index == 0 ? com.liftoff.audio.Sfx.SPARKLE : index == 1 ? com.liftoff.audio.Sfx.SPARKLE_2 : com.liftoff.audio.Sfx.SPARKLE_3);
        vanish.setOnFinished(event -> {
            var fall = drawAll(cascade.after(), cascade.fallFrom());
            fall.setOnFinished(done -> playCascades(cascades, index + 1));
            play(fall);
        });
        play(vanish);
        refreshHud(cascades, index);
    }

    private void checkEnd() {
        if (game.won()) {
            com.liftoff.audio.Audio.get().play(com.liftoff.audio.Sfx.WIN);
            showBanner(RoadMapController.star(34, Theme.AMBER), text.get("match.won"),
                    text.get("match.wonDetail", game.collected()));
            finish.setDisable(false);
            finish.requestFocus();
            status.setText(text.get("match.won"));
        } else if (game.outOfMoves()) {
            com.liftoff.audio.Audio.get().play(com.liftoff.audio.Sfx.LOSE);
            showBanner(new Label("⊘"), text.get("match.lost"), text.get("match.lostDetail"));
            status.setText(text.get("match.lost"));
        } else {
            hint.playFromStart();
        }
    }

    private void showBanner(Node icon, String title, String detail) {
        hint.stop();
        overlay = banner(icon, title, detail);
        boardHost.getChildren().add(overlay);
    }

    @FXML
    private void onRestart() {
        stop();
        newGame();
        board.requestFocus();
    }

    @Override
    protected void stop() {
        hint.stop();
        if (running != null) {
            running.stop();
        }
    }

    // ---- drawing ----

    /**
     * Replaces every tile node with the given kinds. With fall rows, tiles start where they fell
     * from and the returned animation drops them into place.
     */
    private ParallelTransition drawAll(int[][] kinds, int[][] fallFrom) {
        var fall = new ParallelTransition();
        int cols = kinds.length;
        int rows = kinds[0].length;
        tiles = new Node[cols][rows];
        board.getChildren().clear();
        var clip = new Rectangle(cols * TILE, rows * TILE);
        board.setClip(clip);
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                var node = tile(kinds[c][r]);
                node.setLayoutX(c * TILE + 3);
                node.setLayoutY(r * TILE + 3);
                tiles[c][r] = node;
                board.getChildren().add(node);
                if (fallFrom != null && fallFrom[c][r] != r) {
                    int drop = r - fallFrom[c][r];
                    node.setTranslateY(-drop * TILE);
                    var move = new TranslateTransition(ms(130 + 55 * drop), node);
                    move.setToY(0);
                    move.setInterpolator(Interpolator.EASE_IN);
                    fall.getChildren().add(move);
                }
            }
        }
        board.getChildren().add(cursor);
        placeCursor();
        return fall;
    }

    private Node tile(int kind) {
        var face = new StackPane(SpaceTiles.icon(tileSet.get(kind), TILE * .74));
        face.getStyleClass().addAll("match-tile", "match-tile-" + kind);
        face.setPrefSize(TILE - 6, TILE - 6);
        face.setMinSize(TILE - 6, TILE - 6);
        face.setMaxSize(TILE - 6, TILE - 6);
        face.setMouseTransparent(true);
        return face;
    }

    private void placeCursor() {
        cursor.setLayoutX(focus.col() * TILE + 1);
        cursor.setLayoutY(focus.row() * TILE + 1);
        cursor.setVisible(board.isFocused());
        cursor.toFront();
    }

    private void shake(Node node) {
        var shake = new TranslateTransition(ms(60), node);
        shake.setFromX(-6);
        shake.setToX(6);
        shake.setCycleCount(4);
        shake.setAutoReverse(true);
        shake.setOnFinished(event -> node.setTranslateX(0));
        shake.play();
    }

    private void showHint() {
        if (busy || game.over()) {
            return;
        }
        game.board().findMove().ifPresent(move -> {
            for (Cell cell : new Cell[] {move.from(), move.to()}) {
                var pulse = new ScaleTransition(ms(260), tiles[cell.col()][cell.row()]);
                pulse.setToX(1.14);
                pulse.setToY(1.14);
                pulse.setCycleCount(4);
                pulse.setAutoReverse(true);
                pulse.play();
            }
            status.setText(text.get("match.hint"));
        });
    }

    private void play(Animation animation) {
        running = animation;
        animation.play();
    }

    private Duration ms(double millis) {
        return Duration.millis(navigator.reducedMotion() ? Math.min(millis, 40) : millis);
    }

    private void refreshHud() {
        refreshHud(List.of(), -1);
    }

    /** Shows the score; during a cascade, counts the tiles cleared so far. */
    private void refreshHud(List<MatchBoard.Cascade> cascades, int upTo) {
        int shown = game.collected();
        for (int i = upTo + 1; i < cascades.size(); i++) {
            shown -= cascades.get(i).cleared().size();
        }
        shown = Math.max(0, shown);
        collected.setText(text.get("match.collected", Math.min(shown, game.goal()), game.goal()));
        moves.setText(text.get("match.moves", game.movesLeft()));
        goalProgress.setProgress(Math.min(1, shown / (double) game.goal()));
    }

    private String tileName(Cell cell) {
        return text.get("match.tile." + tileSet.get(game.board().kind(cell)));
    }

    private void describeFocus() {
        String where = text.get("match.cursor", focus.row() + 1, focus.col() + 1, tileName(focus));
        board.setAccessibleText(picked == null ? where : where + " " + text.get("match.picked", tileName(picked)));
    }
}
