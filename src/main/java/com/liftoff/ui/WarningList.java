package com.liftoff.ui;

import com.liftoff.engine.DesignIssue;
import com.liftoff.engine.sim.LaunchIssue;
import com.liftoff.render.Icons;
import com.liftoff.session.stub.BuildIssue;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public final class WarningList extends VBox {
    private final UiText text;

    WarningList(UiText text) {
        super(6);
        this.text = text;
        getStyleClass().add("warnings");
    }

    static String describe(UiText text, BuildIssue issue) {
        String slot = issue.slot() == null ? "" : text.get("slot." + issue.slot());
        return text.get("warning." + issue.kind(), slot, issue.needed(), issue.available());
    }

    static String describe(UiText text, DesignIssue issue) {
        String measurement = issue.measurement() == null ? "" : text.get("measurement." + issue.measurement());
        String key = Double.isNaN(issue.available()) ? "design.RESOLUTION_UNKNOWN" : "design." + issue.kind();
        return text.get(key, measurement, issue.needed(), issue.available());
    }


    void showBuildWithoutGauges(List<BuildIssue> issues) {
        showBuild(issues.stream().filter(issue -> switch (issue.kind()) {
            case POWER_SHORT, FUEL_SHORT, OVER_BUDGET -> false;
            default -> true;
        }).toList());
    }

    void showBuild(List<BuildIssue> issues) {
        getChildren().setAll(issues.stream()
                .map(issue -> warning(describe(text, issue), text.get("fix.build." + issue.kind()),
                        Icons.forWarning(issue.kind(), 23))).toList());
    }

    void addDesign(List<DesignIssue> issues) {
        issues.forEach(issue -> getChildren().add(warning(describe(text, issue),
                text.get("fix.design." + issue.kind()), Icons.warning(23))));
    }

    void showLaunch(List<LaunchIssue> issues) {
        getChildren().setAll(issues.stream().map(issue ->
                warning(text.get(issue.messageKey(), issue.needed(), issue.available()),
                        text.get("fix.launch." + issue.kind()), Icons.warning(23))).toList());
    }

    private HBox warning(String content, String action, Node icon) {
        var label = text.label(content, "warning");
        var hint = text.label(text.get("fix.try", action), "warning-action");
        var row = new HBox(8, icon, new VBox(4, label, hint));
        row.getStyleClass().add("warning-card");
        return row;
    }
}
