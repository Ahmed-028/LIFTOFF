package com.liftoff.render3d;

import javafx.scene.shape.TriangleMesh;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Dev tool: brings a Wavefront model under a triangle budget by vertex clustering. Points closer
 * together than a grid cell merge into their average; triangles that collapse disappear. The cell
 * is the smallest that meets the budget, found by bisection, so as much detail as possible stays.
 * Materials are kept, and a material with under a tenth of the triangles is left exactly as it
 * is: thin details such as solar panels would otherwise fold flat. Faces are read through
 * {@link ObjLoader}, triangulated exactly as the game does it, and written back with the original
 * handedness.
 *
 * <p>Usage: {@code ModelSimplifier <file.obj> [maxTriangles]} rewrites the file in place.
 */
public final class ModelSimplifier {

    private ModelSimplifier() {
    }

    public static void main(String[] args) throws IOException {
        Path file = Path.of(args[0]);
        int budget = args.length > 1 ? Integer.parseInt(args[1]) : 4800;
        Map<String, TriangleMesh> meshes;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            meshes = ObjLoader.loadMaterials(reader);
        }
        int before = meshes.values().stream().mapToInt(ModelSimplifier::count).sum();
        Set<String> exact = new HashSet<>();
        meshes.forEach((material, mesh) -> {
            if (count(mesh) < before / 10) {
                exact.add(material);
            }
        });
        double low = 0;
        double high = extent(meshes);
        Map<String, List<int[]>> best = null;
        Map<Long, float[]> bestPoints = null;
        for (int round = 0; round < 40; round++) {
            double cell = (low + high) / 2;
            var clusters = new LinkedHashMap<Long, float[]>();
            var faces = cluster(meshes, cell, exact, clusters);
            int count = faces.values().stream().mapToInt(List::size).sum();
            if (count <= budget) {
                high = cell;
                best = faces;
                bestPoints = clusters;
            } else {
                low = cell;
            }
        }
        write(file, best, bestPoints, before);
    }

    private static int count(TriangleMesh mesh) {
        return mesh.getFaces().size() / mesh.getFaceElementSize();
    }

    private static double extent(Map<String, TriangleMesh> meshes) {
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (TriangleMesh mesh : meshes.values()) {
            for (int i = 0; i < mesh.getPoints().size(); i++) {
                min = Math.min(min, mesh.getPoints().get(i));
                max = Math.max(max, mesh.getPoints().get(i));
            }
        }
        return max - min;
    }

    /** Triangles per material, as cluster keys, with collapsed and repeated triangles removed. */
    private static Map<String, List<int[]>> cluster(Map<String, TriangleMesh> meshes, double cell,
                                                     Set<String> exact, Map<Long, float[]> clusters) {
        var keyIndex = new HashMap<Long, Integer>();
        var sums = new LinkedHashMap<Long, double[]>();
        var result = new LinkedHashMap<String, List<int[]>>();
        Set<String> seen = new HashSet<>();
        int[] meshNumber = {0};
        meshes.forEach((material, mesh) -> {
            long ownKeys = Long.MIN_VALUE + ((long) meshNumber[0]++ << 32);
            boolean keep = exact.contains(material);
            var points = mesh.getPoints();
            var faces = mesh.getFaces();
            var kept = new java.util.ArrayList<int[]>();
            for (int f = 0; f < faces.size(); f += 6) {
                int[] keys = new int[3];
                for (int corner = 0; corner < 3; corner++) {
                    int p = faces.get(f + corner * 2) * 3;
                    float x = points.get(p);
                    float y = points.get(p + 1);
                    float z = points.get(p + 2);
                    long key = keep || cell <= 0 ? ownKeys + p : key(x, y, z, cell);
                    var sum = sums.computeIfAbsent(key, k -> new double[4]);
                    sum[0] += x;
                    sum[1] += y;
                    sum[2] += z;
                    sum[3]++;
                    keys[corner] = keyIndex.computeIfAbsent(key, k -> keyIndex.size());
                }
                if (keys[0] != keys[1] && keys[1] != keys[2] && keys[0] != keys[2]) {
                    int[] sorted = keys.clone();
                    java.util.Arrays.sort(sorted);
                    if (seen.add(material + java.util.Arrays.toString(sorted))) {
                        kept.add(keys);
                    }
                }
            }
            result.put(material, kept);
        });
        sums.forEach((key, sum) -> clusters.put((long) keyIndex.get(key),
                new float[] {(float) (sum[0] / sum[3]), (float) (sum[1] / sum[3]), (float) (sum[2] / sum[3])}));
        return result;
    }

    private static long key(float x, float y, float z, double cell) {
        long i = (long) Math.floor(x / cell) & 0x1FFFFF;
        long j = (long) Math.floor(y / cell) & 0x1FFFFF;
        long k = (long) Math.floor(z / cell) & 0x1FFFFF;
        return (i << 42) | (j << 21) | k;
    }

    /** Writes points and faces, undoing the loader's Y flip and winding swap. Unused points are dropped. */
    private static void write(Path file, Map<String, List<int[]>> faces, Map<Long, float[]> clusters, int before)
            throws IOException {
        var used = new LinkedHashMap<Integer, Integer>();
        faces.values().forEach(list -> list.forEach(face -> {
            for (int key : face) {
                used.computeIfAbsent(key, k -> used.size() + 1);
            }
        }));
        StringBuilder out = new StringBuilder();
        int after = faces.values().stream().mapToInt(List::size).sum();
        out.append(String.format(Locale.ROOT, "# Simplified from %d to %d triangles by ModelSimplifier (vertex clustering).%n",
                before, after));
        used.forEach((key, index) -> {
            float[] p = clusters.get((long) key);
            out.append(String.format(Locale.ROOT, "v %.6f %.6f %.6f%n", p[0], -p[1], p[2]));
        });
        faces.forEach((material, list) -> {
            if (list.isEmpty()) {
                return;
            }
            out.append("usemtl ").append(material).append(System.lineSeparator());
            for (int[] face : list) {
                // The loader stores (a, c, b) for an original (a, b, c).
                out.append("f ").append(used.get(face[0])).append(' ').append(used.get(face[2])).append(' ')
                        .append(used.get(face[1])).append(System.lineSeparator());
            }
        });
        Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
        System.out.println(file.getFileName() + ": " + before + " -> " + after + " triangles");
    }
}
