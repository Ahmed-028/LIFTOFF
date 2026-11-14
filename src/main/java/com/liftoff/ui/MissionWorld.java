package com.liftoff.ui;

import com.liftoff.render.Theme;
import com.liftoff.session.stub.Mission;
import javafx.scene.Node;
import javafx.scene.Parent;


final class MissionWorld {
    private MissionWorld() {}

    static String styleClass(Mission.Destination destination) {
        return Theme.World.of(destination).styleClass();
    }


    static void apply(Node node, Mission.Destination destination) {
        Node top = node;
        for (Parent parent = node.getParent(); parent != null; parent = parent.getParent()) {
            top = parent;
        }
        top.getStyleClass().add(styleClass(destination));
    }
}
