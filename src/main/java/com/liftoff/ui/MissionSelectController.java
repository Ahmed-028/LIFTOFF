package com.liftoff.ui;

import com.liftoff.app.SceneNavigator;
import com.liftoff.app.Screen;
import com.liftoff.app.ScreenController;
import com.liftoff.data.MissionEntry;
import com.liftoff.session.GameSession;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.ResourceBundle;

public final class MissionSelectController implements ScreenController {
    @FXML private HBox mainMissions;
    @FXML private HBox extraMissions;
    private SceneNavigator navigator;

    @Override
    public void attach(SceneNavigator navigator, GameSession session, ResourceBundle messages) {
        this.navigator = navigator;
        var text = new UiText(messages);
        for (MissionEntry mission : session.catalog().missions()) {
            var card = new ObjectiveCard(mission, text, () -> {
                session.selectMission(mission.id());
                navigator.showMissionStep();
            });
            HBox.setHgrow(card, Priority.ALWAYS);
            (mission.ladder() ? mainMissions : extraMissions).getChildren().add(card);
        }
    }

    @FXML
    private void onBack() {
        navigator.show(Screen.TITLE);
    }

    @FXML
    private void onSources() {
        navigator.show(Screen.SOURCES);
    }
}
