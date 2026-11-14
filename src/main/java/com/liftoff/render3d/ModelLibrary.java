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


public final class ModelLibrary {
    static final String RESOURCE_ROOT = "/com/liftoff/models/";

    private final Map<String, Map<String, TriangleMesh>> models = new HashMap<>();
    private final Map<String, PhongMaterial> materials = new HashMap<>();


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
            case "tiles" -> surface("#2A3040");
            case "edge" -> surface("#A9B8CC");
            case "metal" -> surface("#BAC6D4");
            case "glass" -> lit("#1D6F8A", "#0A4E5E");
            case "solar" -> lit("#2F64CF", "#0B2154");
            case "gold" -> surface("#D9AE5E");
            case "orange" -> surface("#FF7A1A");
            case "rubber" -> surface("#1A2232");
            case "aperture" -> lit("#0B2A3A", "#06414F");
            case "cyan" -> lit("#2BE6F5", "#0B7D8C");
            case "nozzle" -> lit("#FFAA52", "#702806");
            default -> surface("#F2F5FA");
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
