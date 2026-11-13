package com.liftoff.render3d;

import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Loads Wavefront geometry. Assets use metres or a consistent set of art units, Y up. */
public final class ObjLoader {
    private ObjLoader() {
    }

    /** Builds a single mesh, with material assignments omitted. The caller owns the reader. */
    public static TriangleMesh load(Reader reader) throws IOException {
        Geometry geometry = parse(reader);
        return geometry.mesh(geometry.faces().values().stream().flatMap(List::stream).toList());
    }

    static Map<String, TriangleMesh> loadMaterials(Reader reader) throws IOException {
        Geometry geometry = parse(reader);
        Map<String, TriangleMesh> meshes = new LinkedHashMap<>();
        geometry.faces().forEach((material, faces) -> meshes.put(material, geometry.mesh(faces)));
        return meshes;
    }

    private static Geometry parse(Reader reader) throws IOException {
        var lines = new BufferedReader(reader);
        var parser = new Parser();
        int lineNumber = 0;
        String line;
        while ((line = lines.readLine()) != null) {
            lineNumber++;
            int comment = line.indexOf('#');
            String content = (comment == -1 ? line : line.substring(0, comment)).strip();
            if (content.isEmpty()) {
                continue;
            }
            String[] tokens = content.split("\\s+");
            try {
                parser.read(tokens);
            } catch (IllegalArgumentException | IndexOutOfBoundsException ex) {
                throw new IOException("OBJ line " + lineNumber + ": invalid " + tokens[0], ex);
            }
        }
        if (parser.faces.isEmpty()) {
            throw new IOException("OBJ contains no faces");
        }
        return new Geometry(parser.points, parser.textures, parser.faces);
    }

    /** What has been read so far: points, texture coordinates and faces grouped by material. */
    private static final class Parser {
        final List<Float> points = new ArrayList<>();
        final List<Float> textures = new ArrayList<>(List.of(0f, 0f));
        final Map<String, List<Integer>> faces = new LinkedHashMap<>();
        String material = "hull";
        int normals;

        void read(String[] tokens) {
            switch (tokens[0]) {
                case "v" -> {
                    points.add(coordinate(tokens, 1));
                    points.add(-coordinate(tokens, 2));
                    points.add(coordinate(tokens, 3));
                }
                case "vt" -> {
                    textures.add(coordinate(tokens, 1));
                    textures.add(tokens.length > 2 ? 1 - coordinate(tokens, 2) : 0f);
                }
                case "vn" -> {
                    coordinate(tokens, 1);
                    coordinate(tokens, 2);
                    coordinate(tokens, 3);
                    normals++;
                }
                case "usemtl" -> material = tokens[1];
                case "f" -> face(tokens);
                default -> {
                    // Object/group names and external material libraries are optional.
                }
            }
        }

        private void face(String[] tokens) {
            List<Corner> corners = new ArrayList<>();
            for (int i = 1; i < tokens.length; i++) {
                corners.add(corner(tokens[i], points.size() / 3, textures.size() / 2 - 1, normals));
            }
            triangulate(corners, points, faces.computeIfAbsent(material, key -> new ArrayList<>()));
        }
    }

    private static float coordinate(String[] tokens, int index) {
        float value = Float.parseFloat(tokens[index]);
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException("Non-finite coordinate");
        }
        return value;
    }

    private static Corner corner(String token, int points, int textures, int normals) {
        String[] indices = token.split("/", -1);
        if (indices.length > 3) {
            throw new IllegalArgumentException("Invalid face corner");
        }
        int point = index(indices[0], points);
        int texture = indices.length >= 2 && !indices[1].isEmpty() ? index(indices[1], textures) + 1 : 0;
        if (indices.length == 3 && !indices[2].isEmpty()) {
            index(indices[2], normals);
        }
        return new Corner(point, texture);
    }

    private static int index(String token, int count) {
        int index = Integer.parseInt(token);
        int resolved = index > 0 ? index - 1 : count + index;
        if (resolved < 0 || resolved >= count) {
            throw new IllegalArgumentException("Index out of range: " + index);
        }
        return resolved;
    }

    private static void triangulate(List<Corner> corners, List<Float> points, List<Integer> faces) {
        if (corners.size() < 3) {
            throw new IllegalArgumentException("A face needs three corners");
        }
        if (corners.size() > 3) {
            List<Point3D> polygon = corners.stream().map(c -> point(points, c.point())).toList();
            Point3D normal = Point3D.ZERO;
            for (int i = 0; i < polygon.size(); i++) {
                normal = normal.add(polygon.get(i).crossProduct(polygon.get((i + 1) % polygon.size())));
            }
            Point3D direction = normal.normalize();
            // Clip convex ears to preserve the interior of concave Wavefront faces.
            var remaining = new ArrayList<>(polygon);
            while (remaining.size() > 3) {
                int ear = findEar(remaining, direction);
                if (ear == -1) {
                    throw new IllegalArgumentException("Degenerate or non-simple face");
                }
                triangle(faces, corners.get((ear + corners.size() - 1) % corners.size()),
                        corners.get(ear), corners.get((ear + 1) % corners.size()));
                remaining.remove(ear);
                corners.remove(ear);
            }
        }
        triangle(faces, corners.get(0), corners.get(1), corners.get(2));
    }

    private static Point3D point(List<Float> points, int index) {
        int i = index * 3;
        return new Point3D(points.get(i), points.get(i + 1), points.get(i + 2));
    }

    private static int findEar(List<Point3D> polygon, Point3D normal) {
        for (int i = 0; i < polygon.size(); i++) {
            int previous = (i + polygon.size() - 1) % polygon.size();
            int next = (i + 1) % polygon.size();
            Point3D a = polygon.get(previous);
            Point3D b = polygon.get(i);
            Point3D c = polygon.get(next);
            if (side(a, b, c, normal) <= 0) {
                continue;
            }
            boolean empty = true;
            for (int j = 0; j < polygon.size(); j++) {
                if (j != previous && j != i && j != next && inside(polygon.get(j), a, b, c, normal)) {
                    empty = false;
                    break;
                }
            }
            if (empty) {
                return i;
            }
        }
        return -1;
    }

    private static boolean inside(Point3D p, Point3D a, Point3D b, Point3D c, Point3D normal) {
        return side(a, b, p, normal) >= 0 && side(b, c, p, normal) >= 0 && side(c, a, p, normal) >= 0;
    }

    private static double side(Point3D a, Point3D b, Point3D p, Point3D normal) {
        return b.subtract(a).crossProduct(p.subtract(a)).dotProduct(normal);
    }

    private static void triangle(List<Integer> faces, Corner a, Corner b, Corner c) {
        // Negating Y reflects the geometry, so reverse the winding.
        for (Corner corner : List.of(a, c, b)) {
            faces.add(corner.point());
            faces.add(corner.texture());
        }
    }

    private record Corner(int point, int texture) {
    }

    private record Geometry(List<Float> points, List<Float> textures, Map<String, List<Integer>> faces) {
        TriangleMesh mesh(List<Integer> indices) {
            var mesh = new TriangleMesh();
            mesh.getPoints().setAll(toArray(points));
            mesh.getTexCoords().setAll(toArray(textures));
            mesh.getFaces().setAll(indices.stream().mapToInt(Integer::intValue).toArray());
            mesh.getFaceSmoothingGroups().setAll(new int[indices.size() / 6]);
            return mesh;
        }

        private static float[] toArray(List<Float> list) {
            float[] array = new float[list.size()];
            for (int i = 0; i < list.size(); i++) {
                array[i] = list.get(i);
            }
            return array;
        }
    }
}
