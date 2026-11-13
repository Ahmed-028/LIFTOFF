package com.liftoff.ui;

import com.liftoff.engine.sim.SimRules;
import com.liftoff.model.Destination;
import com.liftoff.render.Icons;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class BriefingController extends MissionScreen {
    @FXML private Label title;
    @FXML private Label question;
    @FXML private Label description;
    @FXML private VBox limits;
    @FXML private Label difficulty;
    @FXML private Label inspiration;
    @FXML private VBox measurements;
    @FXML private StackPane artwork;

    @Override
    protected void setUp() {
        var mission = mission();
        var goal = mission.briefing();
        title.setText(goal.title());
        question.setText(goal.question());
        description.setText(goal.description());
        difficulty.setText(text.get("mission.difficulty." + mission.difficulty()) + "  ·  "
                + text.get("briefing.eventChance", SimRules.eventChancePerStep(mission.difficulty())));
        String duration = mission.steps() == 0 ? text.get("briefing.minimum", goal.minScienceMonths())
                : text.get("briefing.duration", mission.steps(), text.get("duration." + mission.stepUnit()));
        addLimit(text.get("briefing.budget", goal.budgetCapMusd()), Icons.coin(24));
        addLimit(text.get("briefing.deadline", goal.deadlineMonths()), Icons.clock(24));
        addLimit(duration, Icons.clock(24));
        if (mission.destination() == Destination.EARTH_ORBIT) {
            addLimit(text.get("briefing.latitude", goal.targetLatitudeDeg()), Icons.warning(24));
        } else {
            addLimit(text.get("briefing.deltaV", mission.deltaVRequiredMs()), Icons.fuel(24));
        }
        inspiration.setText(text.get("briefing.basedOn", goal.basedOn()));
        goal.required().forEach(requirement ->
                measurements.getChildren().add(text.label("• " + text.requirement(requirement), "briefing-target")));
        if (!goal.bonus().isEmpty()) {
            measurements.getChildren().add(text.label(text.get("briefing.bonus"), "section-title"));
            goal.bonus().forEach(type ->
                    measurements.getChildren().add(text.label("• " + text.get("measurement." + type), "muted")));
        }
        ObjectiveCard.setArtwork(artwork, mission.id(), 230);
    }

    private void addLimit(String value, javafx.scene.Node icon) {
        var tile = text.label(value, "briefing-limit");
        tile.setGraphic(icon);
        limits.getChildren().add(tile);
    }
}
