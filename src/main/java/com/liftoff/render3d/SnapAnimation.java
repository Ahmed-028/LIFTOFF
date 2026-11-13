package com.liftoff.render3d;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.util.Duration;

final class SnapAnimation {
    private static final double START_SCALE = .84;

    private SnapAnimation() {
    }

    static Timeline play(Node node) {
        setScale(node, START_SCALE);
        var timeline = new Timeline(
                scale(node, 0, START_SCALE),
                scale(node, 112, 1.055),
                scale(node, 200, 1));
        timeline.play();
        return timeline;
    }

    static void finish(Node node, Timeline animation) {
        animation.stop();
        setScale(node, 1);
    }

    private static void setScale(Node node, double scale) {
        node.setScaleX(scale);
        node.setScaleY(scale);
        node.setScaleZ(scale);
    }

    private static KeyFrame scale(Node node, double millis, double factor) {
        return new KeyFrame(Duration.millis(millis),
                new KeyValue(node.scaleXProperty(), factor, Interpolator.EASE_BOTH),
                new KeyValue(node.scaleYProperty(), factor, Interpolator.EASE_BOTH),
                new KeyValue(node.scaleZProperty(), factor, Interpolator.EASE_BOTH));
    }
}
