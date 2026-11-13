package com.liftoff.ui;

import com.liftoff.model.ShipPart;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class PartStrip extends HBox {
    private final UiText text;
    private final Consumer<ShipPart> onChoose;
    private final Consumer<ShipPart> onFocus;
    private final Runnable endDrag;
    private final HBox cards = new HBox(8);
    private final ScrollPane viewport = new ScrollPane(cards);
    private final Button previous;
    private final Button next;
    private final List<PartCard> buttons = new ArrayList<>();

    PartStrip(UiText text, Consumer<ShipPart> onChoose,
              Consumer<ShipPart> onFocus, Runnable endDrag) {
        super(8);
        this.text = text;
        this.onChoose = onChoose;
        this.onFocus = onFocus;
        this.endDrag = endDrag;
        getStyleClass().add("part-strip");
        viewport.getStyleClass().add("clear-scroll");
        viewport.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        viewport.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        viewport.setFitToHeight(true);
        viewport.setPannable(false);
        viewport.setFocusTraversable(false);
        viewport.setMinWidth(0);
        HBox.setHgrow(viewport, Priority.ALWAYS);
        previous = arrow("‹", "ship.previousParts", -1);
        next = arrow("›", "ship.nextParts", 1);
        viewport.viewportBoundsProperty().addListener((property, before, bounds) -> updateArrows());
        viewport.hvalueProperty().addListener((property, before, value) -> updateArrows());
        cards.widthProperty().addListener((property, before, width) -> updateArrows());
        getChildren().addAll(previous, viewport, next);
        updateArrows();
    }

    void setParts(List<ShipPart> parts, String selected) {
        buttons.clear();
        cards.getChildren().clear();
        for (ShipPart part : parts) {
            var card = card(part, buttons.size());
            buttons.add(card);
            cards.getChildren().add(card);
        }
        viewport.setHvalue(0);
        select(selected);
        if (selected != null) {
            buttons.stream().filter(card -> card.getId().equals("part-" + selected)).findFirst()
                    .ifPresent(card -> Platform.runLater(() -> reveal(card)));
        }
    }

    /** A card that follows focus into view, moves with the arrow keys and chooses with Enter. */
    private PartCard card(ShipPart part, int index) {
        var card = new PartCard(part, text, () -> onChoose.accept(part), endDrag);
        card.focusedProperty().addListener((property, before, focused) -> {
            if (focused) {
                reveal(card);
                onFocus.accept(part);
            }
        });
        card.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            switch (event.getCode()) {
                case LEFT, UP -> move(index, -1);
                case RIGHT, DOWN -> move(index, 1);
                case ENTER -> card.fire();
                default -> {
                    return;
                }
            }
            event.consume();
        });
        return card;
    }

    void select(String selected) {
        buttons.forEach(card -> card.markSelected(Objects.equals(card.getId(), "part-" + selected)));
    }

    private Button arrow(String glyph, String key, int direction) {
        var button = new Button(glyph);
        button.getStyleClass().add("strip-arrow");
        button.setAccessibleText(text.get(key));
        button.setOnAction(event -> {
            double excess = cards.getWidth() - viewport.getViewportBounds().getWidth();
            if (excess > 0) {
                viewport.setHvalue(Math.clamp(viewport.getHvalue() + direction * 150 / excess, 0, 1));
            }
        });
        return button;
    }

    private void updateArrows() {
        boolean fits = cards.getWidth() <= viewport.getViewportBounds().getWidth() + 1;
        previous.setDisable(fits || viewport.getHvalue() <= .001);
        next.setDisable(fits || viewport.getHvalue() >= .999);
    }

    private void move(int index, int direction) {
        buttons.get(Math.floorMod(index + direction, buttons.size())).requestFocus();
    }

    private void reveal(PartCard card) {
        if (card.getParent() != cards) {
            return;
        }
        double width = viewport.getViewportBounds().getWidth();
        double excess = cards.getWidth() - width;
        if (excess > 0) {
            double left = viewport.getHvalue() * excess;
            double start = card.getBoundsInParent().getMinX();
            double end = card.getBoundsInParent().getMaxX();
            if (start < left) {
                viewport.setHvalue(start / excess);
            } else if (end > left + width) {
                viewport.setHvalue(Math.min(1, (end - width) / excess));
            }
        }
    }
}
