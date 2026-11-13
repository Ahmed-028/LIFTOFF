package com.liftoff.ui;

import com.liftoff.render.Icons;
import com.liftoff.session.stub.ShipPart;
import javafx.scene.control.Button;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.TransferMode;

public final class PartCard extends Button {
    static final DataFormat PART_ID = new DataFormat("application/x-liftoff-part");

    private final ShipPart part;
    private final UiText text;
    private final Label fitted = new Label();

    PartCard(ShipPart part, UiText text, PartPreview preview, Runnable choose, Runnable endDrag) {
        this.part = part;
        this.text = text;
        setId("part-" + part.id());
        getStyleClass().add("part-card");
        setContentDisplay(ContentDisplay.TOP);
        fitted.setText(text.get("ship.selected"));
        fitted.getStyleClass().add("fitted-badge");
        StackPane.setAlignment(fitted, Pos.TOP_RIGHT);
        setGraphic(new StackPane(preview.thumbnail(part, 100, 46), fitted));
        setWrapText(true);
        setOnAction(event -> choose.run());
        setOnDragDetected(event -> {
            var board = startDragAndDrop(TransferMode.COPY);
            var content = new ClipboardContent();
            content.put(PART_ID, part.id());
            board.setContent(content);
            event.consume();
        });
        setOnDragDone(event -> endDrag.run());
        markSelected(false);
    }

    void markSelected(boolean selected) {
        setText(part.kidName());
        fitted.setVisible(selected);
        getStyleClass().remove("chosen");
        if (selected) {
            getStyleClass().add("chosen");
        }
        setAccessibleText(text.get("step.accessible", part.kidName(),
                text.get(selected ? "ship.fitted" : "ship.available")));
    }
}
