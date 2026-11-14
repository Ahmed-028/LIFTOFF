package com.liftoff.ui;

import com.liftoff.app.SceneNavigator;
import com.liftoff.app.Screen;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.stage.Popup;


final class SettingsMenu {

    private static final double ICON_GRID = 24;

    private SettingsMenu() {
    }

    static Button create(UiText text, SceneNavigator navigator) {
        var button = new Button();
        button.setId("open-settings");
        button.setGraphic(gear(22));
        button.getStyleClass().addAll("secondary", "settings-button");
        button.setAccessibleText(text.get("settings.open"));
        button.setTooltip(new Tooltip(text.get("settings.open")));
        button.setMinWidth(Region.USE_PREF_SIZE);

        var title = new Label(text.get("settings.title"));
        title.getStyleClass().add("settings-title");
        var home = new Button(text.get("settings.mainScreen"));
        home.setId("to-main-screen");
        home.getStyleClass().add("secondary");
        home.setMaxWidth(Double.MAX_VALUE);
        var menu = new VBox(10, title, SoundToggles.create(text), home);
        menu.getStyleClass().add("settings-menu");


        var popup = new Popup();
        popup.setAutoHide(true);
        popup.getContent().add(menu);
        home.setOnAction(event -> {
            popup.hide();
            navigator.show(Screen.TITLE);
        });
        button.setOnAction(event -> {
            if (popup.isShowing()) {
                popup.hide();
                return;
            }
            Bounds at = button.localToScreen(button.getBoundsInLocal());
            if (at != null) {
                popup.show(button, at.getMinX(), at.getMaxY() + 6);
                popup.setX(at.getMaxX() - popup.getWidth());
            }
        });
        return button;
    }


    private static Node gear(double size) {
        double centre = ICON_GRID / 2;
        Shape gear = new Circle(centre, centre, 7.4);
        for (int turn = 0; turn < 4; turn++) {
            var teeth = new Rectangle(centre - 2, 1.5, 4, ICON_GRID - 3);
            teeth.setArcWidth(2);
            teeth.setArcHeight(2);
            teeth.setRotate(turn * 45);
            gear = Shape.union(gear, teeth);
        }
        gear = Shape.subtract(gear, new Circle(centre, centre, 3.2));
        gear.getStyleClass().add("gear-icon");
        gear.setScaleX(size / ICON_GRID);
        gear.setScaleY(size / ICON_GRID);
        return new Group(gear);
    }
}
