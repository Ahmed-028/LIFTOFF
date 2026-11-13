package com.liftoff.ui;

import com.liftoff.engine.BuildIssue;
import com.liftoff.engine.DesignIssue;
import com.liftoff.engine.sim.LaunchIssue;
import com.liftoff.render.Icons;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;

public final class WarningList extends VBox {
    private final UiText text;
    /** What the list shows now, so an unchanged list is not rebuilt. */
    private Object shown;

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
        show(issues, List.of());
    }

    /** The build's warnings, then the design's. Redrawn only when either list changed. */
    void show(List<BuildIssue> build, List<DesignIssue> design) {
        if (unchanged(List.of(build, design))) {
            return;
        }
        getChildren().setAll(build.stream()
                .map(issue -> warning(describe(text, issue), Icons.forWarning(issue.kind(), 18))).toList());
        design.forEach(issue -> getChildren().add(warning(describe(text, issue), Icons.warning(18))));
    }

    void showLaunch(List<LaunchIssue> issues) {
        if (unchanged(issues)) {
            return;
        }
        getChildren().setAll(issues.stream().map(issue ->
                warning(text.get(issue.messageKey(), issue.needed(), issue.available()), Icons.warning(18))).toList());
    }

    private boolean unchanged(Object shown) {
        if (shown.equals(this.shown)) {
            return true;
        }
        this.shown = shown;
        return false;
    }

    private Label warning(String content, Node icon) {
        var label = text.label(content, "warning");
        label.setGraphic(icon);
        label.setAccessibleText(text.get("warning.accessible", content));
        return label;
    }
}
