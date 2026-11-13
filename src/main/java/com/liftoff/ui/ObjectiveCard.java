package com.liftoff.ui;

import com.liftoff.data.MissionEntry;
import com.liftoff.model.Difficulty;
import com.liftoff.render.Images;
import com.liftoff.render.ObjectiveArt;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** A mission card with a full-width destination image and a keyboard-accessible action. */
public final class ObjectiveCard extends VBox {
    ObjectiveCard(MissionEntry mission, UiText text, Runnable onChoose) {
        var goal = mission.briefing();
        getStyleClass().add("objective-card");
        setMaxWidth(Double.MAX_VALUE);
        boolean recommended = mission.ladder() && mission.difficulty() == Difficulty.EASY;
        if (recommended) {
            getStyleClass().add("recommended");
        }

        var art = new StackPane();
        setArtwork(art, mission.id(), 160);
        var title = text.label(goal.title(), "card-title");
        var difficulty = text.label(text.get("mission.difficulty." + mission.difficulty()), "eyebrow");
        var question = text.label(goal.question(), "body-text");
        var choose = new Button(text.get("card.choose"));
        choose.setId("mission-" + mission.id());
        choose.getStyleClass().add(recommended ? "primary" : "secondary");
        choose.setMaxWidth(Double.MAX_VALUE);
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
        getChildren().addAll(art, difficulty, title, question, spacer, choose);
    }

    static void setArtwork(StackPane holder, String missionId, double height) {
        holder.getStyleClass().add("mission-art");
        holder.setMinHeight(height);
        holder.setPrefHeight(height);
        holder.setMaxHeight(height);
        Image image = Images.get("/com/liftoff/art/" + missionId + ".jpg");
        if (image == null) {
            holder.getChildren().setAll(ObjectiveArt.forObjective(missionId, height * .72));
            return;
        }
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
    }
}
