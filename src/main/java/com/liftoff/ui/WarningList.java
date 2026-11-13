package com.liftoff.ui;

import com.liftoff.engine.DesignIssue;
import com.liftoff.engine.sim.LaunchIssue;
import com.liftoff.render.Icons;
import com.liftoff.session.stub.BuildIssue;
import javafx.scene.Node;
import javafx.scene.control.Label;
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

    void showBuild(List<BuildIssue> issues) {
        getChildren().setAll(issues.stream()
                .map(issue -> warning(describe(text, issue), Icons.forWarning(issue.kind(), 18))).toList());
    }

    void addDesign(List<DesignIssue> issues) {
        issues.forEach(issue -> getChildren().add(warning(describe(text, issue), Icons.warning(18))));
    }

    void showLaunch(List<LaunchIssue> issues) {
        getChildren().setAll(issues.stream().map(issue ->
                warning(text.get(issue.messageKey(), issue.needed(), issue.available()), Icons.warning(18))).toList());
    }

    private Label warning(String content, Node icon) {
        var label = text.label(content, "warning");
        label.setGraphic(icon);
        label.setAccessibleText(text.get("warning.accessible", content));
        return label;
    }
}
