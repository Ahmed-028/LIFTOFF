package com.liftoff.ui;

import com.liftoff.render3d.HangarShipView;
import com.liftoff.session.GameSession;
import com.liftoff.session.stub.ModuleSlot;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.util.Optional;


final class BuiltShipView {
    private BuiltShipView() {}


    static Optional<StackPane> create(GameSession session, UiText text, double height) {
        if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
            return Optional.empty();
        }
        var hangar = new HangarShipView();
        for (var slot : ModuleSlot.values()) {
            hangar.showPart(com.liftoff.model.ModuleSlot.valueOf(slot.name()), session.build().part(slot)
                    .map(part -> slot == ModuleSlot.SATELLITE ? PartPreview.modelFor(part) : part.modelId())
                    .orElse(null));
        }
        hangar.setCargoBayOpen(true);
        Node viewport = hangar.node();
        viewport.setAccessibleText(text.get("ship.view3d"));
        var hint = new Label(text.get("ship.hint"));
        hint.getStyleClass().add("hangar-hint");
        hint.setMouseTransparent(true);
        StackPane.setAlignment(hint, javafx.geometry.Pos.TOP_LEFT);
        var frame = new StackPane(viewport, hint);
        frame.getStyleClass().add("hangar-frame");
        frame.setMinHeight(height * .75);
        frame.setPrefHeight(height);
        frame.setMinWidth(0);
        return Optional.of(frame);
    }
}
