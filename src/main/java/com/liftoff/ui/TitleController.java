package com.liftoff.ui;

import com.liftoff.app.SceneNavigator;
import com.liftoff.app.Screen;
import com.liftoff.app.ScreenController;
import com.liftoff.render.RocketArt;
import com.liftoff.session.GameSession;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

import java.util.ResourceBundle;

/** The title screen: the game's name and the way into a mission. */
public final class TitleController implements ScreenController {

    private static final double ROCKET_HEIGHT = 300;

    @FXML
    private StackPane artHolder;

    private SceneNavigator navigator;

    @FXML
    private void initialize() {
        artHolder.getChildren().add(RocketArt.create(ROCKET_HEIGHT));
    }

    @Override
    public void attach(SceneNavigator navigator, GameSession session, ResourceBundle messages) {
        this.navigator = navigator;
    }

    @FXML
    private void onStart() {
        navigator.show(Screen.MISSION_SELECT);
    }
}
