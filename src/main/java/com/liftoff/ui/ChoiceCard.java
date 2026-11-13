package com.liftoff.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * A rounded, bright card around a check box. The whole card toggles the choice, the check box keeps
 * keyboard focus and its accessible state, and the picked state shows a tick, a word and a colour.
 */
final class ChoiceCard extends VBox {
    private final CheckBox option;
    private final Label badge = new Label();
    private final String pickedText;
    private final String openText;

    ChoiceCard(CheckBox option, String pickedText, String openText) {
        this.option = option;
        this.pickedText = pickedText;
        this.openText = openText;
        getStyleClass().add("choice-card");
        setMaxWidth(Double.MAX_VALUE);
        option.setWrapText(true);
        option.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(option, Priority.ALWAYS);
        badge.getStyleClass().add("pick-badge");
        badge.setMinWidth(Region.USE_PREF_SIZE);
        var header = new HBox(12, option, badge);
        header.setAlignment(Pos.CENTER_LEFT);
        getChildren().add(header);

        option.selectedProperty().addListener((property, before, selected) -> refresh());
        option.focusedProperty().addListener((property, before, focused) -> {
            getStyleClass().remove("focused-card");
            if (focused) {
                getStyleClass().add("focused-card");
            }
        });
        addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.PRIMARY && !inside(option, event.getTarget())) {
                option.requestFocus();
                option.fire();
            }
        });
        refresh();
    }

    void add(Node... nodes) {
        getChildren().addAll(nodes);
    }

    static FlowPane chips(Node... chips) {
        var row = new FlowPane(8, 8, chips);
        row.setMinHeight(Region.USE_PREF_SIZE);
        return row;
    }

    static Label chip(String content, Node icon, String style) {
        var chip = new Label(content, icon);
        chip.getStyleClass().add(style);
        chip.setMinWidth(Region.USE_PREF_SIZE);
        return chip;
    }

    private void refresh() {
        boolean picked = option.isSelected();
        badge.setText(picked ? pickedText : openText);
        getStyleClass().remove("chosen");
        if (picked) {
            getStyleClass().add("chosen");
        }
    }

    private static boolean inside(Node owner, Object target) {
        for (var node = target instanceof Node start ? start : null; node != null; node = node.getParent()) {
            if (node == owner) {
                return true;
            }
        }
        return false;
    }
}
