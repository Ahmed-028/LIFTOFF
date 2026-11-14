package com.liftoff.ui;

import com.liftoff.data.MissionEntry;
import com.liftoff.engine.sim.Difficulty;
import com.liftoff.render.Icons;
import com.liftoff.render.ObjectiveArt;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

/** A mission card coloured by its destination, with a full-width image and a keyboard-accessible action. */
public final class ObjectiveCard extends VBox {
    ObjectiveCard(MissionEntry mission, UiText text, Runnable onChoose) {
        var goal = mission.briefing();
        getStyleClass().addAll("objective-card", MissionWorld.styleClass(mission.destination()));
        setMaxWidth(Double.MAX_VALUE);
        boolean recommended = mission.ladder() && mission.difficulty() == Difficulty.CADET;
        if (recommended) {
            getStyleClass().add("recommended");
        }

        var art = new StackPane();
        setArtwork(art, mission.id(), 170);
        roundTop(art, 22);
        if (recommended) {
            var badge = new Label(text.get("card.recommended"));
            badge.getStyleClass().add("recommended-badge");
            StackPane.setAlignment(badge, Pos.TOP_LEFT);
            badge.setTranslateX(14);
            badge.setTranslateY(14);
            art.getChildren().add(badge);
        }
        var difficulty = text.label(text.get("mission.stars." + mission.difficulty()) + "  "
                + text.get("mission.difficulty." + mission.difficulty()), "eyebrow");
        var title = text.label(goal.title(), "card-title");
        var question = text.label(goal.question(), "body-text");
        var facts = new FlowPane(8, 8, chip(text.get("card.budget", goal.budgetCapMusd()), Icons.coin(18)),
                chip(text.get("card.deadline", goal.deadlineMonths()), Icons.clock(18)));
        var inspiration = text.label(text.get("briefing.basedOn", goal.basedOn()), "muted");
        var choose = new Button(text.get(recommended ? "card.start" : "card.choose", goal.title()));
        choose.setId("mission-" + mission.id());
        choose.getStyleClass().add(recommended ? "primary" : "secondary");
        choose.setMaxWidth(Double.MAX_VALUE);
        choose.setMinHeight(54);
        choose.setAccessibleText(text.get("card.chooseAccessible", goal.title()));
        choose.setOnAction(event -> onChoose.run());
        choose.focusedProperty().addListener((property, before, focused) -> {
            if (focused) {
                getStyleClass().add("chosen");
            } else {
                getStyleClass().remove("chosen");
            }
        });
        var spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        var body = new VBox(difficulty, title, question, facts, inspiration, spacer, choose);
        body.getStyleClass().add("card-body");
        VBox.setVgrow(body, Priority.ALWAYS);
        getChildren().addAll(art, body);
    }

    private static Node chip(String content, Node icon) {
        var chip = new Label(content, icon);
        chip.getStyleClass().add("info-chip");
        chip.setMinWidth(Region.USE_PREF_SIZE);
        return chip;
    }

    private static void roundTop(Region holder, double radius) {
        var clip = new Rectangle();
        clip.setArcWidth(radius * 2);
        clip.setArcHeight(radius * 2);
        clip.widthProperty().bind(holder.widthProperty());
        clip.heightProperty().bind(holder.heightProperty().add(radius));
        holder.setClip(clip);
    }

    static void setArtwork(StackPane holder, String missionId, double height) {
        holder.getStyleClass().add("mission-art");
        holder.setMinHeight(height);
        holder.setPrefHeight(height);
        holder.setMaxHeight(height);
        var resource = ObjectiveCard.class.getResource("/com/liftoff/art/" + missionId + ".jpg");
        if (resource == null) {
            holder.getChildren().setAll(ObjectiveArt.forObjective(missionId, height * .72));
            return;
        }
        Image image = new Image(resource.toExternalForm());
        ImageView picture = new ImageView(image);
        picture.setManaged(false);
        picture.setFitHeight(height);
        holder.widthProperty().addListener((property, before, width) -> {
            double fitWidth = width.doubleValue();
            if (fitWidth <= 0) {
                return;
            }
            double cropWidth = Math.min(image.getWidth(), image.getHeight() * fitWidth / height);
            double cropHeight = Math.min(image.getHeight(), image.getWidth() * height / fitWidth);
            picture.setViewport(new Rectangle2D((image.getWidth() - cropWidth) / 2,
                    (image.getHeight() - cropHeight) / 2, cropWidth, cropHeight));
            picture.setFitWidth(fitWidth);
        });
        holder.getChildren().setAll(picture);
        var clip = new Rectangle();
        clip.setArcWidth(36);
        clip.setArcHeight(36);
        clip.widthProperty().bind(holder.widthProperty());
        clip.heightProperty().bind(holder.heightProperty());
        holder.setClip(clip);
    }
}
