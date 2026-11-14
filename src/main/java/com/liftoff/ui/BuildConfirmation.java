package com.liftoff.ui;

import com.liftoff.session.GameSession;
import com.liftoff.session.MissionStep;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;


final class BuildConfirmation {
    private final StackPane gate;
    private final Button button;

    BuildConfirmation(StackPane gate, Button button) {
        this.gate = gate;
        this.button = button;
        gate.setAccessibleRole(AccessibleRole.TEXT);
        gate.focusedProperty().addListener((property, before, focused) -> showWarning(focused));
    }

    void refresh(GameSession session, UiText text) {
        var report = session.buildReport();
        var messages = new ArrayList<String>();
        report.issues().forEach(issue -> messages.add(WarningList.describe(text, issue) + "\n"
                + text.get("fix.try", text.get("fix.build." + issue.kind()))));
        session.reasonLocked(MissionStep.REVIEW).map(text::get).ifPresent(messages::add);
        String reasons = String.join("\n", messages);
        button.setDisable(!report.canConfirm() || !session.isUnlocked(MissionStep.REVIEW));
        gate.setFocusTraversable(button.isDisabled());
        gate.setAccessibleText(reasons.isEmpty() ? null : button.getText() + ". " + reasons);

        var previous = button.getTooltip();
        if (previous != null && previous.getText().equals(reasons)) {
            return;
        }
        if (previous != null) {
            previous.hide();
            Tooltip.uninstall(gate, previous);
        }
        var tooltip = reasons.isEmpty() ? null : new Tooltip(reasons);
        button.setTooltip(tooltip);
        if (tooltip != null) {
            tooltip.setWrapText(true);
            tooltip.setMaxWidth(370);
            Tooltip.install(gate, tooltip);
            showWarning(gate.isFocused());
        }
    }

    private void showWarning(boolean focused) {
        var tooltip = button.getTooltip();
        if (tooltip == null) {
            return;
        }
        if (!focused) {
            tooltip.hide();
            return;
        }
        var bounds = gate.localToScreen(gate.getBoundsInLocal());
        if (bounds != null) {
            tooltip.show(gate, bounds.getMinX(), bounds.getMaxY());
        }
    }

    void hideWarning() {
        showWarning(false);
    }
}
