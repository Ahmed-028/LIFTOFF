package com.liftoff.ui;

import com.liftoff.minigame.RunnerGame;
import com.liftoff.render.DashPainter;
import com.liftoff.render.Theme;
import com.liftoff.session.Level;
import javafx.animation.AnimationTimer;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Level 4: Asteroid Dash, a three-lane ride through the asteroid belt. */
public final class AsteroidDashController extends MiniGameScreen {

    @FXML private StackPane trackHost;
    @FXML private Button leftButton;
    @FXML private Button rightButton;
    @FXML private Button jumpButton;
    @FXML private Button duckButton;
    @FXML private ProgressBar distanceBar;
    @FXML private Label distance;
    @FXML private Label shields;
    @FXML private Label stars;
    @FXML private Button pause;
    @FXML private CheckBox calm;
    @FXML private Button finish;

    private final Canvas canvas = new Canvas();
    private DashPainter painter;
    private RunnerGame game;
    private Node overlay;
    private boolean running;
    private double clock;
    private long last;
    private int shownHits;
    private int shownStars;
    private final EventHandler<KeyEvent> keys = this::onKey;

    private final AnimationTimer loop = new AnimationTimer() {
        @Override
        public void handle(long now) {
            double seconds = last == 0 ? 0 : (now - last) / 1e9;
            last = now;
            seconds = Math.min(seconds, .05);
            clock += seconds;
            game.update(seconds);
            draw();
            refreshHud();
            if (game.over()) {
                end();
            }
        }
    };

    @Override
    protected Level level() {
        return Level.ASTEROID_DASH;
    }

    @FXML private Label difficulty;
    @FXML private Label subtitle;

    @Override
    protected void setUp() {
        painter = new DashPainter(mission().destination());
        subtitle.setText(text.get("dash.subtitle." + mission().destination().name()));
        difficulty.setText(difficultyText());
        canvas.widthProperty().bind(trackHost.widthProperty().subtract(24));
        canvas.heightProperty().bind(trackHost.heightProperty().subtract(24));
        canvas.widthProperty().addListener((property, previous, width) -> draw());
        canvas.heightProperty().addListener((property, previous, height) -> draw());
        // Unmanaged, so the canvas follows the frame's size instead of pushing it wider.
        canvas.setManaged(false);
        canvas.setLayoutX(12);
        canvas.setLayoutY(12);
        canvas.setFocusTraversable(true);
        canvas.setAccessibleText(text.get("dash.track"));
        trackHost.getChildren().add(canvas);
        leftButton.setOnAction(event -> act(() -> game.left()));
        rightButton.setOnAction(event -> act(() -> game.right()));
        jumpButton.setOnAction(event -> act(() -> game.jump()));
        duckButton.setOnAction(event -> act(() -> game.duck()));
        calm.setSelected(navigator.reducedMotion());
        calm.selectedProperty().addListener((property, previous, enabled) -> {
            navigator.setReducedMotion(enabled);
            if (!running && !game.over() && game.distance() == 0) {
                game = new RunnerGame(System.nanoTime(), challenge(), enabled);
                draw();
            }
        });
        trackHost.sceneProperty().addListener((property, previous, scene) -> {
            if (previous != null) {
                previous.removeEventFilter(KeyEvent.KEY_PRESSED, keys);
            }
            if (scene != null) {
                scene.addEventFilter(KeyEvent.KEY_PRESSED, keys);
            }
        });
        newGame();
    }

    private void newGame() {
        running = false;
        loop.stop();
        game = new RunnerGame(System.nanoTime(), challenge(), navigator.reducedMotion());
        clock = 0;
        last = 0;
        shownHits = 0;
        shownStars = 0;
        finish.setDisable(true);
        pause.setDisable(true);
        pause.setText(text.get("dash.pause"));
        refreshHud();
        draw();
        var start = new Button(text.get("dash.start"));
        start.setId("start-ride");
        start.getStyleClass().addAll("secondary", "big");
        start.setOnAction(event -> startRide());
        showOverlay(banner(RoadMapController.star(30, Theme.AMBER), text.get("dash.ready"), text.get("dash.readyDetail")), start);
        start.requestFocus();
    }

    private void startRide() {
        hideOverlay();
        com.liftoff.audio.Audio.get().play(com.liftoff.audio.Sfx.WHOOSH);
        running = true;
        last = 0;
        pause.setDisable(false);
        canvas.requestFocus();
        loop.start();
    }

    @FXML
    private void onPause() {
        if (game.over()) {
            return;
        }
        if (running) {
            running = false;
            loop.stop();
            pause.setText(text.get("dash.resume"));
            showOverlay(banner(new Label("Ⅱ"), text.get("dash.paused"), ""), null);
        } else {
            pause.setText(text.get("dash.pause"));
            startRide();
        }
    }

    @FXML
    private void onRestart() {
        hideOverlay();
        newGame();
    }

    private void end() {
        running = false;
        loop.stop();
        pause.setDisable(true);
        com.liftoff.audio.Audio.get().play(game.finished() ? com.liftoff.audio.Sfx.WIN : com.liftoff.audio.Sfx.LOSE);
        if (game.finished()) {
            finish.setDisable(false);
            showOverlay(banner(RoadMapController.star(34, Theme.AMBER), text.get("dash.won"),
                    text.get("dash.wonDetail", game.stars())), null);
            finish.requestFocus();
        } else {
            showOverlay(banner(new Label("⊘"), text.get("dash.lost"), text.get("dash.lostDetail")), null);
        }
    }

    private void onKey(KeyEvent event) {
        if (!running) {
            return;
        }
        switch (event.getCode()) {
            case LEFT, A -> game.left();
            case RIGHT, D -> game.right();
            case UP, W, SPACE -> game.jump();
            case DOWN, S -> game.duck();
            case P, ESCAPE -> onPause();
            default -> {
                return;
            }
        }
        event.consume();
    }

    private void act(Runnable move) {
        if (running) {
            move.run();
        }
        canvas.requestFocus();
    }

    private void showOverlay(Node card, Button action) {
        hideOverlay();
        if (action != null && card instanceof VBox box) {
            box.getChildren().add(action);
        }
        overlay = card;
        trackHost.getChildren().add(card);
    }

    private void hideOverlay() {
        if (overlay != null) {
            trackHost.getChildren().remove(overlay);
            overlay = null;
        }
    }

    private void draw() {
        if (game == null || canvas.getWidth() <= 0 || canvas.getHeight() <= 0) {
            return;
        }
        painter.paint(canvas.getGraphicsContext2D(), canvas.getWidth(), canvas.getHeight(), game, clock);
    }

    private void refreshHud() {
        distanceBar.setProgress(game.progress());
        distance.setText(text.get("dash.distance", game.distance(), game.finish()));
        String hearts = "♥".repeat(Math.max(0, game.shields())) + "♡".repeat(RunnerGame.SHIELDS - Math.max(0, game.shields()));
        shields.setText(hearts + "  " + text.get("dash.shields", Math.max(0, game.shields())));
        stars.setText(text.get("dash.stars", game.stars()));
        if (game.stars() != shownStars) {
            shownStars = game.stars();
            com.liftoff.audio.Audio.get().play(com.liftoff.audio.Sfx.STAR);
        }
        if (game.hits() != shownHits) {
            shownHits = game.hits();
            com.liftoff.audio.Audio.get().play(com.liftoff.audio.Sfx.HIT);
            shields.setAccessibleText(text.get("dash.hit") + " " + text.get("dash.shields", game.shields()));
        }
    }

    @Override
    protected void stop() {
        running = false;
        loop.stop();
    }

    @Override
    public void dispose() {
        if (trackHost.getScene() != null) {
            trackHost.getScene().removeEventFilter(KeyEvent.KEY_PRESSED, keys);
        }
        super.dispose();
    }
}
