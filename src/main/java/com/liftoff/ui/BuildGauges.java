package com.liftoff.ui;

import com.liftoff.engine.ResourceType;
import com.liftoff.engine.ResourceUsage;
import com.liftoff.render.Icons;
import com.liftoff.session.GameSession;
import com.liftoff.session.SwapAdvisor;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.Duration;
import javafx.geometry.Pos;
import javafx.animation.PauseTransition;
import javafx.scene.Node;
import javafx.stage.Popup;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Locale;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

public final class BuildGauges extends StackPane {
    private final UiText text;
    private final boolean expanded;
    private final Pane gauges;
    private GameSession session;
    private Popup openTips;
    private static final Duration TIPS_SHOWN_FOR = Duration.seconds(5);

    BuildGauges(UiText text, boolean expanded) {
        this.text = text;
        this.expanded = expanded;
        getStyleClass().add("gauges");
        gauges = expanded ? new VBox(8) : new HBox(8);
        setAlignment(Pos.TOP_LEFT);
        getChildren().add(gauges);
    }

    void refresh(GameSession session) {
        this.session = session;
        var resources = session.missionResources();
        var build = session.buildReport();
        hideTips();
        gauges.getChildren().clear();
        add(resources.get(ResourceType.MASS));
        add(resources.get(ResourceType.BUDGET));
        add(resources.get(ResourceType.POWER));
        double needed = build.deltaVRequiredMs();
        double available = build.deltaVAvailableMs();
        String fuel = needed == 0 ? text.get("gauge.noBurn")
                : text.get("gauge.fuelValue", build.propellantKg(), available, needed);
        double fuelFraction = available <= 0 ? 0 : needed / available;
        addGauge(text.get("gauge.fuel"), fuel, build.fuelLevel().name(), fuelFraction, Icons.fuel(17),
                SwapAdvisor.Goal.FUEL);
        if (expanded) {
            add(resources.get(ResourceType.DATA));
            add(resources.get(ResourceType.COMMS));
            add(resources.get(ResourceType.TIME));
        }
    }

    private void add(ResourceUsage usage) {
        OptionalDouble cap = usage.capacity();
        String value = cap.isPresent()
                ? text.get("gauge.amountWithLimit", usage.used(), cap.getAsDouble(), usage.type().unit())
                : text.get("gauge.amount", usage.used(), usage.type().unit());
        double fraction = cap.isPresent() && cap.getAsDouble() > 0 ? usage.used() / cap.getAsDouble() : 0;
        addGauge(text.get("resource." + usage.type()), value, usage.level().name(),
                fraction, Icons.forResource(usage.type(), 17), goal(usage.type()));
    }

    private static SwapAdvisor.Goal goal(ResourceType type) {
        return switch (type) {
            case MASS -> SwapAdvisor.Goal.MASS;
            case BUDGET -> SwapAdvisor.Goal.BUDGET;
            case POWER -> SwapAdvisor.Goal.POWER;
            default -> null;
        };
    }

    private void addGauge(String name, String amount, String state, double fraction, Node icon, SwapAdvisor.Goal goal) {
        var label = text.label(name, "gauge-label");
        label.setGraphic(icon);
        var reading = text.label(amount, "gauge-reading");
        Node status = goal != null && "OVER".equals(state) ? fixButton(name, goal)
                : text.label(text.get("level." + state), "gauge-state");
        status.getStyleClass().addAll("gauge-state", "state-" + state.toLowerCase(java.util.Locale.ROOT));
        var bar = new ProgressBar(Math.clamp(fraction, 0, 1));
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setMinHeight(6);
        bar.setPrefHeight(6);
        bar.getStyleClass().add("state-" + state.toLowerCase(java.util.Locale.ROOT));
        bar.setAccessibleText(name + " " + amount + " " + text.get("level." + state));
        var gauge = new VBox(2, label, reading, bar, status);
        if (!expanded) {
            HBox.setHgrow(gauge, Priority.ALWAYS);
            gauge.setPrefWidth(200);
            gauge.setMinWidth(0);
            reading.setWrapText(false);
            reading.setTooltip(new javafx.scene.control.Tooltip(amount));
            gauge.getStyleClass().add("compact-gauge");
        }
        gauges.getChildren().add(gauge);
    }


    private Button fixButton(String resource, SwapAdvisor.Goal goal) {
        var button = new Button(text.get("tips.button"));
        button.setId("fix-" + goal.name().toLowerCase(Locale.ROOT));
        button.getStyleClass().add("gauge-fix");
        button.setMinWidth(Region.USE_PREF_SIZE);
        button.setAccessibleText(text.get("tips.accessible", resource));
        button.setOnAction(event -> {
            if (openTips != null) {
                hideTips();
            } else {
                showTips(button, resource, goal);
            }
        });
        return button;
    }

    private void showTips(Button button, String resource, SwapAdvisor.Goal goal) {
        Bounds at = button.localToScreen(button.getBoundsInLocal());
        if (at == null) {
            return;
        }
        var tips = new Popup();
        tips.setAutoHide(true);
        var content = tipsContent(resource, goal);
        content.getStyleClass().add("tips-popup");
        tips.getContent().add(content);
        var timer = new PauseTransition(TIPS_SHOWN_FOR);
        timer.setOnFinished(event -> tips.hide());
        tips.setOnHidden(event -> {
            timer.stop();
            if (openTips == tips) {
                openTips = null;
            }
        });
        openTips = tips;
        tips.show(button, at.getMinX(), at.getMaxY() + 4);
        timer.play();
    }

    private void hideTips() {
        if (openTips != null) {
            var tips = openTips;
            openTips = null;
            tips.hide();
        }
    }

    private VBox tipsContent(String resource, SwapAdvisor.Goal goal) {
        var box = new VBox(8);
        box.getStyleClass().add("limit-tips");

        box.setMinWidth(360);
        box.setPrefWidth(360);
        box.setMaxWidth(360);
        box.setMaxHeight(Region.USE_PREF_SIZE);
        box.getChildren().add(text.label(text.get("tips.title", resource), "tips-title"));
        var swaps = session.limitTips(goal);
        if (swaps.isEmpty()) {
            box.getChildren().add(text.label(text.get("tips.noSwap"), "tips-line"));
        }
        for (var swap : swaps) {
            var line = text.label(text.get("tips.swap", text.get("slot." + swap.from().slot()),
                    swap.from().kidName(), swap.to().kidName()), "tips-swap");
            var effect = text.label(swap.fixes() ? text.get("tips.fixes")
                    : text.get("tips.helps." + goal.name(), swap.gain()), swap.fixes() ? "tips-fixes" : "tips-line");
            var tip = new VBox(2, line, effect);
            if (!swap.newProblems().isEmpty()) {
                tip.getChildren().add(text.label(text.get("tips.butThen", swap.newProblems().stream()
                        .map(kind -> text.get("tips.problem." + kind.name())).collect(Collectors.joining(", "))),
                        "tips-caution"));
            }
            tip.getStyleClass().add("tips-card");
            box.getChildren().add(tip);
        }
        box.getChildren().add(text.label(text.get("tips.general." + goal.name()), "tips-line"));
        return box;
    }
}
