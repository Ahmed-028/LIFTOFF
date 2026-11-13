package com.liftoff.ui;

import com.liftoff.model.ModuleSlot;
import com.liftoff.model.ShipBuild;
import com.liftoff.render.Icons;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class ModuleTabs extends VBox {
    private final List<ToggleButton> buttons = new ArrayList<>();
    private final UiText text;

    ModuleTabs(UiText text, Consumer<ModuleSlot> onSelect) {
        super(6);
        this.text = text;
        var group = new ToggleGroup();
        for (var slot : ModuleSlot.values()) {
            var button = new ToggleButton();
            button.setId("slot-" + slot.name());
            button.setToggleGroup(group);
            button.setMaxWidth(Double.MAX_VALUE);
            button.getStyleClass().add("module-tab");
            button.setOnAction(event -> {
                button.setSelected(true);
                onSelect.accept(slot);
            });
            button.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                switch (event.getCode()) {
                    case UP, LEFT -> move(slot.ordinal(), -1);
                    case DOWN, RIGHT -> move(slot.ordinal(), 1);
                    case ENTER -> button.fire();
                    default -> { return; }
                }
                event.consume();
            });
            buttons.add(button);
            getChildren().add(button);
        }
    }

    private void move(int index, int direction) {
        var button = buttons.get(Math.floorMod(index + direction, buttons.size()));
        button.requestFocus();
        button.fire();
    }

    void refresh(ModuleSlot active, ShipBuild build) {
        for (var slot : ModuleSlot.values()) {
            var button = buttons.get(slot.ordinal());
            boolean filled = build.part(slot).isPresent();
            String name = text.get("slot." + slot);
            String status = text.get(filled ? "ship.fitted" : "ship.empty");
            button.setText(name + "\n" + status);
            button.setGraphic(filled ? Icons.tick(18) : Icons.forModule(slot, 18));
            button.setSelected(active == slot);
            button.setAccessibleText(text.get("step.accessible", name, status));
        }
    }
}
