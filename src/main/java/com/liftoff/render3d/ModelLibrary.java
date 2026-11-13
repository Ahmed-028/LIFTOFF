package com.liftoff.render3d;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * The 3D models, read once per model id and shared by every view. Files are parsed on one
 * background thread, in the order asked for: {@link #preload} adds to the back of the queue,
 * {@link #prioritize} jumps the front, so the current mission's models are ready first.
 *
 * <p>{@link #create} hands out new scene nodes around the shared, read-only meshes and materials.
 * It never parses a file on the calling thread: a model that is not ready yet is waited for. A
 * model that cannot be read becomes a labelled box, and the game carries on.
 */
public final class ModelLibrary {
    static final String RESOURCE_ROOT = "/com/liftoff/models/";

    private static final Pattern VALID_ID = Pattern.compile("[a-zA-Z0-9][a-zA-Z0-9_-]*");
    private static final Logger LOG = Logger.getLogger("com.liftoff");

    /** Meshes by model id, then by material name. A failed load completes with null. */
    private static final Map<String, CompletableFuture<Map<String, TriangleMesh>>> MODELS = new ConcurrentHashMap<>();
    private static final Map<String, PhongMaterial> MATERIALS = new ConcurrentHashMap<>();
    private static final LinkedBlockingDeque<String> QUEUE = new LinkedBlockingDeque<>();
    private static final AtomicLong FX_WAITS = new AtomicLong();

    static {
        Thread loader = new Thread(ModelLibrary::loadForever, "liftoff-model-loader");
        loader.setDaemon(true);
        loader.setPriority(Thread.NORM_PRIORITY - 1);
        loader.start();
    }

    /** Asks for the models to be read in the background, after anything already asked for. */
    public static CompletableFuture<Void> preload(Collection<String> modelIds) {
        return request(modelIds, false);
    }

    /** Asks for the models to be read before anything else still waiting. */
    public static CompletableFuture<Void> prioritize(Collection<String> modelIds) {
        return request(modelIds, true);
    }

    public static boolean isReady(String modelId) {
        var model = MODELS.get(modelId);
        return model != null && model.isDone();
    }

    /** True if the model was looked for and could not be read; it is shown as a labelled box. */
    public static boolean failed(String modelId) {
        var model = MODELS.get(modelId);
        return model != null && model.isDone() && model.join() == null;
    }

    /** How often a view on the FX thread had to wait for a model that was not ready yet. */
    public static long waitsOnFxThread() {
        return FX_WAITS.get();
    }

    /** New nodes for the model; meshes and materials are shared and must not be changed. */
    public Group create(String modelId) {
        if (modelId == null || !VALID_ID.matcher(modelId).matches()) {
            throw new IllegalArgumentException("Invalid model ID: " + modelId);
        }
        var model = request(java.util.List.of(modelId), true);
        if (!model.isDone() && Platform.isFxApplicationThread()) {
            FX_WAITS.incrementAndGet();
        }
        model.join();
        Map<String, TriangleMesh> meshes = MODELS.get(modelId).join();
        if (meshes == null) {
            return placeholder(modelId);
        }
        var copy = new Group();
        meshes.forEach((material, mesh) -> {
            var view = new MeshView(mesh);
            view.setMaterial(MATERIALS.computeIfAbsent(material, ModelLibrary::paint));
            copy.getChildren().add(view);
        });
        return copy;
    }

    private static CompletableFuture<Void> request(Collection<String> modelIds, boolean first) {
        var waiting = modelIds.stream().filter(id -> id != null && VALID_ID.matcher(id).matches()).map(id -> {
            boolean[] added = {false};
            var model = MODELS.computeIfAbsent(id, key -> {
                added[0] = true;
                return new CompletableFuture<>();
            });
            if (!model.isDone() && (added[0] || first)) {
                if (first) {
                    QUEUE.addFirst(id);
                } else {
                    QUEUE.addLast(id);
                }
            }
            return model;
        }).toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(waiting);
    }

    private static void loadForever() {
        while (true) {
            try {
                String id = QUEUE.takeFirst();
                var model = MODELS.get(id);
                if (model != null && !model.isDone()) {
                    model.complete(read(id));
                }
            } catch (InterruptedException stop) {
                return;
            } catch (RuntimeException | Error unexpected) {
                LOG.log(Level.SEVERE, "Model loader problem", unexpected);
            }
        }
    }

    /** The model's meshes, or null if it cannot be read. Runs on the loader thread. */
    private static Map<String, TriangleMesh> read(String modelId) {
        String resource = RESOURCE_ROOT + modelId + ".obj";
        var stream = ModelLibrary.class.getResourceAsStream(resource);
        if (stream == null) {
            LOG.warning("Model not found, showing a box instead: " + resource);
            return null;
        }
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return ObjLoader.loadMaterials(reader);
        } catch (IOException | RuntimeException problem) {
            LOG.log(Level.WARNING, "Model cannot be read, showing a box instead: " + resource, problem);
            return null;
        }
    }

    /** What a model that cannot be read looks like: a plain box with its id written on it. */
    private static Group placeholder(String modelId) {
        var box = new Box(1.2, .6, 1.2);
        box.setMaterial(MATERIALS.computeIfAbsent("missing", ModelLibrary::paint));
        var label = new Text(modelId);
        label.setFont(Font.font(12));
        label.setFill(Color.WHITE);
        label.setScaleX(.03);
        label.setScaleY(.03);
        label.setTranslateX(-label.getLayoutBounds().getWidth() / 2);
        label.setTranslateY(-.35);
        label.setTranslateZ(-.61);
        return new Group(box, label);
    }

    private static PhongMaterial paint(String material) {
        return switch (material) {
            case "tiles" -> surface("#273850");
            case "edge" -> surface("#7389A4");
            case "metal" -> surface("#9CAEBF");
            case "glass" -> surface("#163F61");
            case "solar" -> surface("#264DA7");
            case "gold" -> surface("#D3A656");
            case "orange" -> surface("#EC802B");
            case "rubber" -> surface("#141C2A");
            case "aperture" -> surface("#0B1829");
            case "missing" -> surface("#5B6577");
            case "cyan" -> lit("#24DCF1", "#086372");
            case "nozzle" -> lit("#FFAA52", "#702806");
            default -> surface("#E3E9F1");
        };
    }

    static PhongMaterial surface(String hex) {
        var material = new PhongMaterial(Color.web(hex));
        material.setSpecularColor(Color.web("#425773"));
        material.setSpecularPower(24);
        return material;
    }

    static PhongMaterial lit(String color, String emission) {
        var material = surface(color);
        var image = new WritableImage(1, 1);
        image.getPixelWriter().setColor(0, 0, Color.web(emission));
        material.setSelfIlluminationMap(image);
        return material;
    }
}
