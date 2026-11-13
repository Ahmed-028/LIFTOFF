package com.liftoff.ui;

import com.liftoff.model.ShipPart;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.TransferMode;

public final class PartCard extends Button {
    static final DataFormat PART_ID = new DataFormat("application/x-liftoff-part");

    private final ShipPart part;
    private final UiText text;

    PartCard(ShipPart part, UiText text, Runnable choose, Runnable endDrag) {
        this.part = part;
        this.text = text;
        setId("part-" + part.id());
        getStyleClass().add("part-card");
        setContentDisplay(ContentDisplay.TOP);
        setGraphic(PartPreview.thumbnail(part, 100, 46));
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
        setText(part.kidName() + (selected ? "\n" + text.get("ship.selected") : ""));
        getStyleClass().remove("chosen");
        if (selected) {
            getStyleClass().add("chosen");
        }
        setAccessibleText(text.get("step.accessible", part.kidName(),
                text.get(selected ? "ship.fitted" : "ship.available")));
    }
}
