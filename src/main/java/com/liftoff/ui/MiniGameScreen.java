package com.liftoff.ui;

import com.liftoff.session.Level;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.List;


abstract class MiniGameScreen extends MissionScreen {

    @Override
    protected abstract Level level();

    @Override
    protected boolean showsStepper() {
        return false;
    }

    @FXML
    protected void onMap() {
        navigator.showRoadMap(List.of());
    }

    @FXML
    protected void onSkip() {
        finishLevel();
    }

    @FXML
    protected void onFinish() {
        finishLevel();
    }

    private void finishLevel() {
        stop();
        session.finishMiniGame(level());
        navigator.finishLevel(level());
    }


    protected com.liftoff.minigame.Challenge challenge() {
        return com.liftoff.minigame.Challenge.forTier(session.difficulty().ordinal());
    }

    protected String difficultyText() {
        var challenge = challenge();
        return text.get("game.difficulty", "★".repeat(challenge.stars()) + "☆".repeat(3 - challenge.stars()),
                text.get("game.difficulty." + challenge.name()));
    }


    protected void stop() {
    }

    @Override
    public void dispose() {
        stop();
        super.dispose();
    }


    static void fit(StackPane host, Region content, double width, double height, double maxScale) {
        Runnable fit = () -> {
            double scale = Math.min(maxScale, Math.min(host.getWidth() / (width + 24), host.getHeight() / (height + 24)));
            content.setScaleX(Math.max(.35, scale));
            content.setScaleY(Math.max(.35, scale));
        };
        host.widthProperty().addListener((property, previous, value) -> fit.run());
        host.heightProperty().addListener((property, previous, value) -> fit.run());
    }


    static Node banner(Node icon, String title, String detail) {
        var heading = new javafx.scene.control.Label(title);
        heading.getStyleClass().add("game-banner-title");
        var text = new javafx.scene.control.Label(detail);
        text.getStyleClass().add("game-banner-text");
        text.setWrapText(true);
        var box = new javafx.scene.layout.VBox(10, icon, heading, text);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        box.getStyleClass().add("game-banner");
        box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        box.setPrefWidth(420);
        return box;
    }
}
