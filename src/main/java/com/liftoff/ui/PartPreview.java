package com.liftoff.ui;

import com.liftoff.render.Icons;
import com.liftoff.render3d.ModelLibrary;
import com.liftoff.session.stub.ShipPart;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Rotate;

import java.util.HashMap;
import java.util.Map;

final class PartPreview {
    private static final Map<String, String> NASA_MODELS = Map.of(
            "eo-1-bus", "nasa-eo1", "landsat-7-bus", "nasa-landsat7", "sorce-bus", "nasa-sorce",
            "lunar-lander", "nasa-apollo", "mars-helicopter", "nasa-ingenuity", "mars-rover", "nasa-perseverance");
    private final ModelLibrary models = new ModelLibrary();
    private final Map<String, Image> images = new HashMap<>();

    Node thumbnail(ShipPart part, double width, double height) {
        if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
            var photo = PartPreview.class.getResource("/com/liftoff/art/nasa/"
                    + modelFor(part).replaceFirst("^nasa-", "") + ".jpg");
            if (photo != null) {
                return new ImageView(new Image(photo.toExternalForm(), width, height, true, true));
            }
            return Icons.forModule(part.slot(), height * .7);
        }
        return thumbnail(modelFor(part), width, height);
    }

    static String modelFor(ShipPart part) {
        return NASA_MODELS.getOrDefault(part.modelId(), part.modelId());
    }

    Node thumbnail(String modelId, double width, double height) {
        var picture = new ImageView(images.computeIfAbsent(modelId, this::render));
        picture.setPreserveRatio(true);
        picture.setFitWidth(width);
        picture.setFitHeight(height);
        picture.setMouseTransparent(true);
        return picture;
    }

    private Image render(String modelId) {
        Group part = models.create(modelId);
        var bounds = part.getBoundsInLocal();
        part.setTranslateX(-bounds.getCenterX());
        part.setTranslateY(-bounds.getCenterY());
        part.setTranslateZ(-bounds.getCenterZ());
        var turntable = new Group(part);
        turntable.getTransforms().addAll(new Rotate(-24, Rotate.X_AXIS), new Rotate(-35, Rotate.Y_AXIS));
        double scale = 1 / Math.max(bounds.getWidth(), Math.max(bounds.getHeight(), bounds.getDepth()));
        var model = new Group(turntable);
        model.getTransforms().add(new javafx.scene.transform.Scale(scale, scale, scale));

        var light = new PointLight(Color.WHITE);
        light.setTranslateX(-2);
        light.setTranslateY(-3);
        light.setTranslateZ(-4);
        var camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-2.65);
        var world = new Group(model, camera, new AmbientLight(Color.web("#8A9EB6")), light);
        var scene = new SubScene(world, 256, 172, true, SceneAntialiasing.BALANCED);
        scene.setCamera(camera);
        scene.setFill(Color.TRANSPARENT);
        var holder = new StackPane(scene);
        holder.setStyle("-fx-background-color: #091833");
        var preview = new Scene(holder, 256, 172, Color.web("#091833"));
        return preview.snapshot(null);
    }
}
