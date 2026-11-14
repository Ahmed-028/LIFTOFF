package com.liftoff.ui;

import com.liftoff.session.GameSession;
import com.liftoff.session.Level;
import com.liftoff.session.MissionStep;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class StepperBar extends HBox {
    private static final List<MissionStep> DESIGN_STEPS = List.of(
            MissionStep.BRIEFING, MissionStep.SHIP, MissionStep.SUPPLIES, MissionStep.REVIEW);

    enum State { CURRENT, OPEN, LOCKED }

    private final GameSession session;
    private final UiText text;
    private final EnumMap<MissionStep, Button> buttons = new EnumMap<>(MissionStep.class);

    /** Shows the design steps inside the given level, so the pills never skip past a road-map level. */
    StepperBar(GameSession session, UiText text, Consumer<MissionStep> navigate, Level level) {
        super(6);
        this.session = session;
        this.text = text;
        getStyleClass().add("stepper");
        for (var step : DESIGN_STEPS) {
            if (!level.steps().contains(step)) {
                continue;
            }
            var button = new Button();
            button.getStyleClass().add("step");
            button.setOnAction(event -> navigate.accept(step));
            buttons.put(step, button);
            getChildren().add(button);
        }
    }

    /** A level with one step or none needs no step pills. */
    boolean hasSteps() {
        return buttons.size() > 1;
    }

    static State state(MissionStep step, MissionStep current, Optional<String> reasonLocked) {
        if (step == current) {
            return State.CURRENT;
        }
        return reasonLocked.isPresent() && step.ordinal() > current.ordinal() ? State.LOCKED : State.OPEN;
    }

    void refresh() {
        buttons.forEach((step, button) -> {
            var reason = session.reasonLocked(step);
            var state = state(step, session.step(), reason);
            String name = text.get("step." + step.name());
            String status = text.get("step.state." + state.name());
            button.setText(text.get("step.symbol." + state.name()) + " " + name);
            button.setAccessibleText(text.get("step.accessible", name, status));
            button.setTooltip(new Tooltip(state == State.LOCKED ? text.get(reason.orElseThrow()) : status));
            button.setDisable(state != State.OPEN);
            button.getStyleClass().remove("current-step");
            if (state == State.CURRENT) {
                button.getStyleClass().add("current-step");
            }
        });
    }
}
