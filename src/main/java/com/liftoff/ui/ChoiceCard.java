package com.liftoff.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Supplier;


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
            if (event.getButton() == MouseButton.PRIMARY && !inside(option, event.getTarget())
                    && !onOtherButton(event.getTarget())) {
                option.requestFocus();
                option.fire();
            }
        });
        refresh();
    }

    void add(Node... nodes) {
        getChildren().addAll(nodes);
    }


    void addDetails(String id, String moreText, String lessText, Supplier<List<Node>> content) {
        var details = new VBox(10);
        details.getStyleClass().add("card-details");
        details.setVisible(false);
        details.setManaged(false);
        var toggle = new Hyperlink(moreText);
        toggle.setId(id);
        toggle.getStyleClass().add("details-toggle");
        toggle.setAccessibleText(moreText);
        toggle.setOnAction(event -> {
            boolean open = !details.isVisible();
            if (open && details.getChildren().isEmpty()) {
                details.getChildren().setAll(content.get());
            }
            details.setVisible(open);
            details.setManaged(open);
            toggle.setText(open ? lessText : moreText);
            toggle.setAccessibleText(open ? lessText : moreText);
            toggle.setVisited(false);
            getStyleClass().remove("expanded");
            if (open) {
                getStyleClass().add("expanded");
            }
        });
        getChildren().addAll(toggle, details);
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


    private boolean onOtherButton(Object target) {
        for (var node = target instanceof Node start ? start : null; node != null && node != this; node = node.getParent()) {
            if (node instanceof ButtonBase && node != option) {
                return true;
            }
        }
        return false;
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
