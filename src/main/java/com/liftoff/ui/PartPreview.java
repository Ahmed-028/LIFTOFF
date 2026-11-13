package com.liftoff.ui;

import com.liftoff.model.Catalog;
import com.liftoff.model.ShipPart;
import com.liftoff.render.Icons;
import com.liftoff.render.Images;
import com.liftoff.render3d.ModelLibrary;
import javafx.animation.AnimationTimer;
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
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.transform.Rotate;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pictures of parts for the cards and the detail panel, rendered once per model and kept for the
 * whole game. Rendering needs the FX thread, so {@link #prepare} renders the chosen mission's
 * pictures one per frame while the player reads the briefing, and the builder opens with them ready.
 */
final class PartPreview {
    private static final Map<String, String> NASA_MODELS = Map.of(
            "eo-1-bus", "nasa-eo1", "landsat-7-bus", "nasa-landsat7", "sorce-bus", "nasa-sorce",
            "lunar-lander", "nasa-apollo", "mars-helicopter", "nasa-ingenuity", "mars-rover", "nasa-perseverance");
    private static final ModelLibrary MODELS = new ModelLibrary();
    private static final Map<String, Image> IMAGES = new HashMap<>();
    private static final Set<String> WAITING = new LinkedHashSet<>();
    private static final AnimationTimer WARMER = new AnimationTimer() {
        @Override
        public void handle(long now) {
            renderOneWaiting();
        }
    };

    private PartPreview() {
    }

    /**
     * Gets a mission ready to build: its models jump the loading queue, and their pictures are
     * rendered in the background, one per frame. Call on the FX thread.
     */
    static void prepare(Catalog catalog, String missionId) {
        List<ShipPart> parts = catalog.shipParts().stream().filter(part -> part.offeredIn(missionId)).toList();
        Set<String> models = new LinkedHashSet<>();
        parts.forEach(part -> {
            models.add(modelFor(part));
            models.add(part.modelId());
        });
        ModelLibrary.prioritize(models);
        if (Platform.isSupported(ConditionalFeature.SCENE3D)) {
            parts.stream().map(PartPreview::modelFor).filter(id -> !IMAGES.containsKey(id)).forEach(WAITING::add);
            WARMER.start();
        }
    }

    /** Renders the first waiting picture whose model has been read, so one frame does one picture. */
    private static void renderOneWaiting() {
        var ready = WAITING.stream().filter(ModelLibrary::isReady).findFirst();
        ready.ifPresent(id -> {
            WAITING.remove(id);
            IMAGES.computeIfAbsent(id, PartPreview::render);
        });
        if (WAITING.isEmpty()) {
            WARMER.stop();
        }
    }

    static Node thumbnail(ShipPart part, double width, double height) {
        if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
            var photo = Images.get("/com/liftoff/art/nasa/" + modelFor(part).replaceFirst("^nasa-", "") + ".jpg",
                    width, height);
            return photo != null ? new ImageView(photo) : Icons.forModule(part.slot(), height * .7);
        }
        return thumbnail(modelFor(part), width, height);
    }

    static String modelFor(ShipPart part) {
        return NASA_MODELS.getOrDefault(part.modelId(), part.modelId());
    }

    static Node thumbnail(String modelId, double width, double height) {
        WAITING.remove(modelId);
        var picture = new ImageView(IMAGES.computeIfAbsent(modelId, PartPreview::render));
        picture.setPreserveRatio(true);
        picture.setFitWidth(width);
        picture.setFitHeight(height);
        picture.setMouseTransparent(true);
        return picture;
    }

    private static Image render(String modelId) {
        Group part = MODELS.create(modelId);
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
