package com.liftoff.ui;

import com.liftoff.model.Objective;
import com.liftoff.model.Requirement;
import com.liftoff.render.Icons;
import com.liftoff.render.ObjectiveArt;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/** A card describing one mission objective, with a button to choose it. */
public final class ObjectiveCard extends VBox {

    private static final double ART_SIZE = 96;
    private static final double ICON_SIZE = 28;
    private static final String CHOSEN_STYLE = "chosen";

    private final ResourceBundle messages;
    private final Button chooseButton;

    public ObjectiveCard(Objective objective, ResourceBundle messages, Runnable onChoose) {
        this.messages = messages;
        getStyleClass().add("objective-card");

        Label title = new Label(objective.title());
        title.getStyleClass().add("card-title");

        Label question = new Label(objective.question());
        question.getStyleClass().add("card-question");
        wrap(question);

        Label needsHeading = new Label(messages.getString("card.needs"));
        needsHeading.getStyleClass().add("card-label");

        Label needs = new Label(objective.required().stream()
                .map(this::describe)
                .collect(Collectors.joining("\n")));
        needs.getStyleClass().add("card-needs");
        wrap(needs);

        HBox limits = new HBox(
                limit(Icons.coin(ICON_SIZE), format("card.budget", objective.budgetCapMusd())),
                limit(Icons.clock(ICON_SIZE), format("card.deadline", objective.deadlineMonths())));
        limits.getStyleClass().add("card-limits");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        chooseButton = new Button(messages.getString("card.choose"));
        chooseButton.getStyleClass().add("primary");
        chooseButton.setMaxWidth(Double.MAX_VALUE);
        chooseButton.setOnAction(event -> onChoose.run());
        chooseButton.setAccessibleText(messages.getString("card.choose") + " " + objective.title());

        getChildren().addAll(ObjectiveArt.forObjective(objective.id(), ART_SIZE), title, question,
                needsHeading, needs, limits, spacer, chooseButton);
    }

    /** Marks this card as the chosen mission: a ring around the card, a tick and the word on the button. */
    public void setChosen(boolean chosen) {
        getStyleClass().remove(CHOSEN_STYLE);
        if (chosen) {
            getStyleClass().add(CHOSEN_STYLE);
            chooseButton.setText(messages.getString("card.chosen"));
            chooseButton.setGraphic(Icons.tick(ICON_SIZE));
        } else {
            chooseButton.setText(messages.getString("card.choose"));
            chooseButton.setGraphic(null);
        }
    }

    private String describe(Requirement requirement) {
        String name = messages.getString("measurement." + requirement.type().name());
        if (requirement.maxResolutionM() == null) {
            return name;
        }
        return format("card.needs.withDetail", name, requirement.maxResolutionM());
    }

    /** Lets a label run onto as many lines as it needs instead of being cut off with dots. */
    private static void wrap(Label label) {
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMinHeight(Region.USE_PREF_SIZE);
    }

    private HBox limit(Node icon, String text) {
        Label label = new Label(text);
        label.getStyleClass().add("card-limit");
        HBox box = new HBox(icon, label);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setSpacing(8);
        return box;
    }

    private String format(String key, Object... values) {
        return MessageFormat.format(messages.getString(key), values);
    }
}
