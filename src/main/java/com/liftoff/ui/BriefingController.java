package com.liftoff.ui;

import com.liftoff.session.stub.Mission;
import com.liftoff.render.Icons;
import com.liftoff.engine.sim.SimRules;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class BriefingController extends MissionScreen {
    @FXML private Label title;
    @FXML private Label question;
    @FXML private Label description;
    @FXML private FlowPane limits;
    @FXML private Label difficulty;
    @FXML private Label objective;
    @FXML private Label inspiration;
    @FXML private VBox measurements;
    @FXML private StackPane artwork;

    @Override
    protected void setUp() {
        var mission = mission();
        var goal = mission.briefing();
        title.setText(goal.title());
        String goalKey = "goal." + mission.id();
        objective.setText(text.bundle().containsKey(goalKey) ? text.get(goalKey) : goal.description());
        question.setText(goal.question());
        description.setText(goal.description());
        difficulty.setText(text.get("mission.stars." + mission.difficulty()) + "  "
                + text.get("mission.difficulty." + mission.difficulty()) + "  ·  "
                + text.get("briefing.eventChance", SimRules.eventChancePerStep(mission.difficulty())));
        String duration = mission.steps() == 0 ? text.get("briefing.minimum", goal.minScienceMonths())
                : text.get("briefing.duration", mission.steps(), text.get("duration." + mission.stepUnit()));
        addLimit(text.get("briefing.budget", goal.budgetCapMusd()), Icons.coin(24));
        addLimit(text.get("briefing.deadline", goal.deadlineMonths()), Icons.clock(24));
        addLimit(duration, Icons.clock(24));
        if (mission.destination() == Mission.Destination.EARTH) {
            addLimit(text.get("briefing.latitude", goal.targetLatitudeDeg()), Icons.warning(24));
        } else {
            addLimit(text.get("briefing.deltaV", mission.deltaVRequiredMs()), Icons.fuel(24));
        }
        inspiration.setText(text.get("briefing.basedOn", goal.basedOn()));
        goal.required().forEach(requirement ->
                measurements.getChildren().add(goalRow(text.requirement(requirement), true)));
        goal.bonus().forEach(type ->
                measurements.getChildren().add(goalRow(text.get("measurement." + type), false)));
        ObjectiveCard.setArtwork(artwork, mission.id(), 220);
    }

    private Node goalRow(String measurement, boolean required) {
        var badge = new Label(text.get(required ? "briefing.must" : "briefing.bonusTag"));
        badge.getStyleClass().add("goal-badge");
        badge.setMinWidth(Region.USE_PREF_SIZE);
        var name = text.label(measurement, required ? "briefing-target" : "body-text");
        HBox.setHgrow(name, Priority.ALWAYS);
        var row = new HBox(badge, name);
        row.getStyleClass().add("goal-row");
        if (!required) {
            row.getStyleClass().add("bonus");
        }
        return row;
    }

    private void addLimit(String value, Node icon) {
        var tile = text.label(value, "briefing-limit");
        tile.setGraphic(icon);
        tile.setMaxWidth(Region.USE_PREF_SIZE);
        limits.getChildren().add(tile);
    }
}
