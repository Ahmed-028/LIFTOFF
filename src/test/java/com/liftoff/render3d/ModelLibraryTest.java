package com.liftoff.render3d;

import com.liftoff.FxTest;
import com.liftoff.data.CatalogLoader;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.text.Text;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ModelLibraryTest {

    private static int triangles(javafx.scene.Group model) {
        return model.getChildren().stream()
                .filter(MeshView.class::isInstance)
                .map(node -> (TriangleMesh) ((MeshView) node).getMesh())
                .mapToInt(mesh -> mesh.getFaces().size() / mesh.getFaceElementSize())
                .sum();
    }

    @Test
    void nasaVariantsLoadWithRenderableGeometry() {
        var library = new ModelLibrary();
        for (String model : List.of("eo1", "landsat7", "sorce", "apollo", "ingenuity",
                "perseverance", "mastcam", "pixl", "rimfax")) {
            var instance = library.create("nasa-" + model);
            assertFalse(instance.getChildren().isEmpty(), model);
            assertTrue(instance.getBoundsInLocal().getWidth() > 0, model);
            assertTrue(instance.getChildren().stream().allMatch(MeshView.class::isInstance), model + " fell back");
        }
    }

    @Test
    void everyModelFileLoadsWithinTheTriangleBudget() throws Exception {
        var library = new ModelLibrary();
        var folder = java.nio.file.Path.of("src", "main", "resources", "com", "liftoff", "models");
        List<String> ids;
        try (var files = java.nio.file.Files.list(folder)) {
            ids = files.map(path -> path.getFileName().toString()).filter(name -> name.endsWith(".obj"))
                    .map(name -> name.substring(0, name.length() - 4)).sorted().toList();
        }
        assertTrue(ids.size() >= 30, "found only " + ids);
        for (String id : ids) {
            int count = triangles(library.create(id));
            assertTrue(count > 0 && count < 5000, id + ": " + count + " triangles (doc 06: under about 5,000)");
        }
    }

    @Test
    void copiesShareGeometryWithoutSharingSceneNodes() {
        var left = new ModelLibrary().create("cubesat-3u");
        var right = new ModelLibrary().create("cubesat-3u");

        assertNotSame(left, right);
        var leftMesh = (MeshView) left.getChildren().getFirst();
        var rightMesh = (MeshView) right.getChildren().getFirst();
        assertNotSame(leftMesh, rightMesh);
        assertSame(leftMesh.getMesh(), rightMesh.getMesh(), "one mesh per model, shared by every library");
        assertSame(leftMesh.getMaterial(), rightMesh.getMaterial(), "materials are cached too");
        assertSame(left, leftMesh.getParent());
        assertSame(right, rightMesh.getParent());
    }

    @Test
    void preloadedModelsAreNeverParsedOnTheFxThread() throws Exception {
        var catalog = CatalogLoader.loadFromClasspath();
        List<String> ids = catalog.shipParts().stream().map(part -> part.modelId()).distinct().toList();
        ModelLibrary.preload(ids).get(30, TimeUnit.SECONDS);
        ids.forEach(id -> assertTrue(ModelLibrary.isReady(id), id));

        FxTest.run(() -> {
            long before = ModelLibrary.waitsOnFxThread();
            var library = new ModelLibrary();
            ids.forEach(library::create);
            assertEquals(before, ModelLibrary.waitsOnFxThread(), "a preloaded model was waited for");
        });
    }

    @Test
    void aMissionsModelsJumpTheQueue() throws Exception {
        ModelLibrary.prioritize(List.of("nasa-ingenuity")).get(30, TimeUnit.SECONDS);
        assertTrue(ModelLibrary.isReady("nasa-ingenuity"));
    }

    @Test
    void aModelThatCannotBeLoadedBecomesALabelledBoxAndTheGameCarriesOn() {
        var model = new ModelLibrary().create("no-such-model");

        assertFalse(model.getChildren().isEmpty());
        assertTrue(model.getBoundsInLocal().getWidth() > 0);
        assertTrue(model.getChildren().stream().anyMatch(node ->
                node instanceof Text text && text.getText().equals("no-such-model")), "the box is labelled");
        assertTrue(ModelLibrary.failed("no-such-model"));
        assertNotSame(model, new ModelLibrary().create("no-such-model"), "each use gets its own nodes");
    }

    @Test
    void aMalformedIdIsAProgrammingErrorNotAMissingModel() {
        assertThrows(IllegalArgumentException.class, () -> new ModelLibrary().create("../etc/passwd"));
        assertThrows(IllegalArgumentException.class, () -> new ModelLibrary().create(null));
    }
}
