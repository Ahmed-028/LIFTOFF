package com.liftoff.ui;

import com.liftoff.session.stub.ShipPart;
import javafx.geometry.Pos;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class PartStrip extends HBox {
    private final UiText text;
    private final PartPreview preview;
    private final Consumer<ShipPart> onChoose;
    private final Consumer<ShipPart> onFocus;
    private final Runnable endDrag;
    private final List<PartCard> buttons = new ArrayList<>();

    /** One row holding every part of the open module: the cards narrow to fit, so nothing scrolls. */
    PartStrip(UiText text, PartPreview preview, Consumer<ShipPart> onChoose,
              Consumer<ShipPart> onFocus, Runnable endDrag) {
        super(8);
        this.text = text;
        this.preview = preview;
        this.onChoose = onChoose;
        this.onFocus = onFocus;
        this.endDrag = endDrag;
        getStyleClass().add("part-strip");
        setAlignment(Pos.CENTER);
        setMinWidth(0);
    }

    void setParts(List<ShipPart> parts, String selected) {
        buttons.clear();
        for (ShipPart part : parts) {
            var card = new PartCard(part, text, preview, () -> onChoose.accept(part), endDrag);
            int index = buttons.size();
            card.focusedProperty().addListener((property, before, focused) -> {
                if (focused) {
                    onFocus.accept(part);
                }
            });
            card.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                switch (event.getCode()) {
                    case LEFT, UP -> move(index, -1);
                    case RIGHT, DOWN -> move(index, 1);
                    case ENTER -> card.fire();
                    default -> { return; }
                }
                event.consume();
            });
            buttons.add(card);
        }
        getChildren().setAll(buttons);
        select(selected);
    }

    void select(String selected) {
        buttons.forEach(card -> card.markSelected(Objects.equals(card.getId(), "part-" + selected)));
    }

    private void move(int index, int direction) {
        buttons.get(Math.floorMod(index + direction, buttons.size())).requestFocus();
    }
}
