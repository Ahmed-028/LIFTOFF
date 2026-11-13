package com.liftoff.render3d;

import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelLibraryTest {
    @Test
    void everyDemoModelLoadsWithinTheTriangleBudget() {
        var library = new ModelLibrary();
        HangarDemo.MODELS.values().stream().flatMap(java.util.Collection::stream).forEach(id -> {
            var instance = library.create(id);
            int triangles = instance.getChildren().stream()
                    .map(node -> (TriangleMesh) ((MeshView) node).getMesh())
                    .mapToInt(mesh -> mesh.getFaces().size() / mesh.getFaceElementSize())
                    .sum();
            assertTrue(triangles > 0 && triangles < 5000, id + ": " + triangles);
        });
    }

    @Test
    void copiesShareGeometryWithoutSharingSceneNodes() {
        var library = new ModelLibrary();
        var left = library.create("cubesat-3u");
        var right = library.create("cubesat-3u");

        assertNotSame(left, right);
        var leftMesh = (MeshView) left.getChildren().getFirst();
        var rightMesh = (MeshView) right.getChildren().getFirst();
        assertNotSame(leftMesh, rightMesh);
        assertSame(leftMesh.getMesh(), rightMesh.getMesh());
        assertSame(left, leftMesh.getParent());
        assertSame(right, rightMesh.getParent());
    }
}
