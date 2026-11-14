package com.liftoff.render3d;

import com.liftoff.model.ModuleSlot;
import javafx.geometry.Insets;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PointLight;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;


public final class HangarShipView implements ShipView {
    private final StackPane container = new StackPane();
    private final OrbitCamera camera = new OrbitCamera();
    private final ShipAssembly ship = new ShipAssembly(new ModelLibrary());
    private final SlotPicker picker;

    public HangarShipView() {
        var background = HangarShipView.class.getResource(ModelLibrary.RESOURCE_ROOT + "hangar.png");
        var fallback = new BackgroundFill(Color.web("#07122B"), CornerRadii.EMPTY, Insets.EMPTY);
        var backdrop = new BackgroundImage(new Image(background.toExternalForm()), BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER,
                new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, false, true));
        container.setBackground(new Background(new BackgroundFill[]{fallback}, new BackgroundImage[]{backdrop}));
        container.setPrefSize(960, 600);
        container.setMinSize(0, 0);

        var world = new Group(pad(), ship.node(), camera.rig(),
                new AmbientLight(Color.web("#8796B6")),
                light("#BDB6AA", -3, -8, -6),
                light("#527DA8", 6, -5, 4));
        var viewport = new SubScene(world, 1, 1, true, SceneAntialiasing.BALANCED);
        viewport.setFill(Color.TRANSPARENT);
        viewport.widthProperty().bind(container.widthProperty());
        viewport.heightProperty().bind(container.heightProperty());
        container.getChildren().add(viewport);
        camera.install(viewport);
        picker = new SlotPicker(viewport, camera, ship);
    }

    @Override
    public Node node() {
        return container;
    }

    @Override
    public void showPart(ModuleSlot slot, String modelId) {
        if (ship.showPart(slot, modelId) && container.getScene() != null) {
            ship.animate(slot);
        }
    }

    @Override
    public void setCargoBayOpen(boolean open) {
        ship.setCargoBayOpen(open, container.getScene() != null);
    }

    @Override
    public void setDropHighlight(ModuleSlot slot) {
        ship.setDropHighlight(slot);
    }

    @Override
    public ModuleSlot slotAt(double sceneX, double sceneY) {
        return picker.slotAt(sceneX, sceneY);
    }


    public void setCalmMode(boolean calmMode) {
        ship.setCalmMode(calmMode);
    }

    private static PointLight light(String color, double x, double y, double z) {
        var light = new PointLight(Color.web(color));
        light.setTranslateX(x);
        light.setTranslateY(y);
        light.setTranslateZ(z);
        return light;
    }

    private static Group pad() {
        var base = new Cylinder(6.6, .45, 64);
        base.setMaterial(ModelLibrary.surface("#15263F"));
        base.setTranslateY(1.66);
        var deck = new Cylinder(6.25, .18, 64);
        var deckPaint = ModelLibrary.surface("#202D40");
        deckPaint.setSpecularColor(Color.web("#172234"));
        deck.setMaterial(deckPaint);
        deck.setTranslateY(1.35);
        var group = new Group(base, deck);
        var amber = ModelLibrary.lit("#FFB03A", "#BE4C08");
        var blue = ModelLibrary.lit("#42D9F0", "#0D718B");
        for (int i = 0; i < 32; i++) {
            double angle = i * 360.0 / 32;
            double radians = Math.toRadians(angle);
            var marker = new Box(.97, .035, .12);
            marker.setMaterial(i % 8 < 2 ? amber : blue);
            marker.setTranslateX(6.08 * Math.sin(radians));
            marker.setTranslateZ(6.08 * Math.cos(radians));
            marker.setTranslateY(1.25);
            marker.setRotationAxis(Rotate.Y_AXIS);
            marker.setRotate(angle);
            group.getChildren().add(marker);
        }
        group.setMouseTransparent(true);
        return group;
    }
}
