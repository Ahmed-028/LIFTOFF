package com.liftoff.ui;

import com.liftoff.engine.ResourceType;
import com.liftoff.engine.ResourceUsage;
import com.liftoff.render.Icons;
import com.liftoff.session.GameSession;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.OptionalDouble;

public final class BuildGauges extends StackPane {
    private final UiText text;
    private final boolean expanded;
    private final Pane gauges;

    BuildGauges(UiText text, boolean expanded) {
        this.text = text;
        this.expanded = expanded;
        getStyleClass().add("gauges");
        gauges = expanded ? new VBox(8) : new HBox(8);
        setAlignment(Pos.TOP_LEFT);
        getChildren().add(gauges);
    }

    void refresh(GameSession session) {
        var resources = session.missionResources();
        var build = session.buildReport();
        gauges.getChildren().clear();
        add(resources.get(ResourceType.MASS));
        add(resources.get(ResourceType.BUDGET));
        add(resources.get(ResourceType.POWER));
        double needed = build.deltaVRequiredMs();
        double available = build.deltaVAvailableMs();
        String fuel = needed == 0 ? text.get("gauge.noBurn")
                : text.get("gauge.fuelValue", build.propellantKg(), available, needed);
        double fuelFraction = available <= 0 ? 0 : needed / available;
        addGauge(text.get("gauge.fuel"), fuel, build.fuelLevel().name(), fuelFraction, Icons.fuel(17));
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
                fraction, Icons.forResource(usage.type(), 17));
    }

    private void addGauge(String name, String amount, String state, double fraction, Node icon) {
        var label = text.label(name, "gauge-label");
        label.setGraphic(icon);
        var reading = text.label(amount, "gauge-reading");
        var status = text.label(text.get("level." + state), "gauge-state");
        status.getStyleClass().add("state-" + state.toLowerCase(java.util.Locale.ROOT));
        var bar = new ProgressBar(Math.clamp(fraction, 0, 1));
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setMinHeight(6);
        bar.setPrefHeight(6);
        bar.getStyleClass().add("state-" + state.toLowerCase(java.util.Locale.ROOT));
        bar.setAccessibleText(name + " " + amount + " " + status.getText());
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
}
