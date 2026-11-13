package com.liftoff.render3d;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ObjLoaderTest {
    @Test
    void triangulatesCubeWithoutDuplicatingItsVertices() throws IOException {
        var mesh = ObjLoader.load(new StringReader("""
                v -1 -1 -1
                v  1 -1 -1
                v  1  1 -1
                v -1  1 -1
                v -1 -1  1
                v  1 -1  1
                v  1  1  1
                v -1  1  1
                f 1 4 3 2
                f 5 6 7 8
                f 1 2 6 5
                f 4 8 7 3
                f 1 5 8 4
                f 2 3 7 6
                """));
        assertEquals(8, mesh.getPoints().size() / mesh.getPointElementSize());
        assertEquals(12, mesh.getFaces().size() / mesh.getFaceElementSize());
    }

    @Test
    void acceptsRelativeIndicesTextureCoordinatesAndNormals() throws IOException {
        var mesh = ObjLoader.load(new StringReader("""
                v 0 0 0
                v 1 0 0
                v 0 1 0
                vt 0.25 0.75
                vn 0 0 1
                f -3/-1/1 -2//1 -1/1/1 # clockwise after the Y conversion
                """));
        assertEquals(-1f, mesh.getPoints().get(7));
        assertEquals(0.25f, mesh.getTexCoords().get(3));
        assertEquals(2, mesh.getFaces().get(2));
    }

    @Test
    void reportsInvalidIndexWithLineNumber() {
        var error = assertThrows(IOException.class, () -> ObjLoader.load(
                new StringReader("v 0 0 0\nf 1 0 2\n")));
        org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().startsWith("OBJ line 2:"));
    }
}
