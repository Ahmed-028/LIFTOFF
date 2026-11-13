package com.liftoff.ui;

import com.liftoff.model.ModuleSlot;
import com.liftoff.render.Icons;
import com.liftoff.render3d.ShipView;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.EnumMap;

/** Keeps the builder usable on systems where JavaFX cannot draw 3D. */
final class SlotShipView implements ShipView {
    private final StackPane panel;
    private final EnumMap<ModuleSlot, Label> targets = new EnumMap<>(ModuleSlot.class);
    private final UiText text;

    SlotShipView(UiText text) {
        this.text = text;
        var rows = new VBox(7, text.label(text.get("ship.fallback"), "muted"));
        rows.setAlignment(Pos.CENTER);
        for (var slot : ModuleSlot.values()) {
            var label = new Label();
            label.getStyleClass().add("slot-target");
            label.setMinWidth(210);
            label.setGraphic(Icons.forModule(com.liftoff.session.stub.ModuleSlot.valueOf(slot.name()), 18));
            targets.put(slot, label);
            showPart(slot, null);
            rows.getChildren().add(label);
        }
        panel = new StackPane(rows);
        panel.getStyleClass().add("slot-viewport");
        setCargoBayOpen(false);
    }

    @Override
    public Node node() {
        return panel;
    }

    @Override
    public void showPart(ModuleSlot slot, String modelId) {
        String name = text.get("slot." + slot);
        targets.get(slot).setText(text.get("step.accessible", name, text.get(modelId == null ? "ship.empty" : "ship.fitted")));
    }

    @Override
    public void setCargoBayOpen(boolean open) {
        var payload = targets.get(ModuleSlot.SATELLITE);
        payload.setVisible(open);
        payload.setManaged(open);
    }

    @Override
    public void setDropHighlight(ModuleSlot slot) {
        targets.forEach((candidate, target) -> {
            target.getStyleClass().remove("chosen");
            if (candidate == slot) {
                target.getStyleClass().add("chosen");
            }
        });
    }

    @Override
    public ModuleSlot slotAt(double sceneX, double sceneY) {
        return targets.entrySet().stream().filter(entry -> entry.getValue().isVisible()
                && entry.getValue().contains(entry.getValue().sceneToLocal(sceneX, sceneY)))
                .map(java.util.Map.Entry::getKey).findFirst().orElse(null);
    }
}
