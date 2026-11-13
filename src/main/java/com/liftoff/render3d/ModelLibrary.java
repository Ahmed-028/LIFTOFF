package com.liftoff.render3d;

import javafx.scene.Group;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Caches geometry and materials, and returns fresh scene graph nodes on every request. */
public final class ModelLibrary {
    static final String RESOURCE_ROOT = "/com/liftoff/models/";

    private final Map<String, Map<String, TriangleMesh>> models = new HashMap<>();
    private final Map<String, PhongMaterial> materials = new HashMap<>();

    /** Creates independent nodes. Their cached meshes and materials are shared and read-only. */
    public Group create(String modelId) {
        if (modelId == null || !modelId.matches("[a-zA-Z0-9][a-zA-Z0-9_-]*")) {
            throw new IllegalArgumentException("Invalid model ID: " + modelId);
        }
        var copy = new Group();
        models.computeIfAbsent(modelId, ModelLibrary::load).forEach((name, mesh) -> {
            var view = new MeshView(mesh);
            view.setMaterial(materials.computeIfAbsent(name, ModelLibrary::paint));
            copy.getChildren().add(view);
        });
        return copy;
    }

    private static Map<String, TriangleMesh> load(String modelId) {
        String resource = RESOURCE_ROOT + modelId + ".obj";
        var stream = ModelLibrary.class.getResourceAsStream(resource);
        if (stream == null) {
            throw new IllegalArgumentException("Unknown model ID: " + modelId);
        }
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return ObjLoader.loadMaterials(reader);
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not load model " + resource, ex);
        }
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
