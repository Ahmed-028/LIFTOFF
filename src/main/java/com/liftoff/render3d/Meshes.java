package com.liftoff.render3d;

import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;

import java.util.SplittableRandom;

/** Small procedural meshes (rings, turned shapes, rocks and flat screens) for the flight scenes. */
final class Meshes {
    private Meshes() {
    }

    /**
     * A surface of revolution around the Y axis.
     *
     * @param profile pairs of (radius, y), listed from the top of the shape to the bottom
     */
    static TriangleMesh lathe(double[] profile, int segments) {
        int rings = profile.length / 2;
        var mesh = new TriangleMesh();
        for (int i = 0; i < rings; i++) {
            double radius = Math.max(.0005, profile[i * 2]);
            double y = profile[i * 2 + 1];
            for (int j = 0; j < segments; j++) {
                double angle = Math.TAU * j / segments;
                mesh.getPoints().addAll((float) (Math.cos(angle) * radius), (float) y, (float) (Math.sin(angle) * radius));
            }
            for (int j = 0; j <= segments; j++) {
                mesh.getTexCoords().addAll((float) j / segments, (float) i / Math.max(1, rings - 1));
            }
        }
        for (int i = 0; i < rings - 1; i++) {
            for (int j = 0; j < segments; j++) {
                int a = i * segments + j;
                int b = i * segments + (j + 1) % segments;
                int c = (i + 1) * segments + j;
                int d = (i + 1) * segments + (j + 1) % segments;
                int ta = i * (segments + 1) + j;
                int tb = ta + 1;
                int tc = (i + 1) * (segments + 1) + j;
                int td = tc + 1;
                mesh.getFaces().addAll(a, ta, b, tb, c, tc);
                mesh.getFaces().addAll(b, tb, d, td, c, tc);
            }
        }
        return mesh;
    }

    /** A ring lying in the XZ plane (its axis is Y). */
    static TriangleMesh torus(double major, double minor, int majorSegments, int minorSegments) {
        var mesh = new TriangleMesh();
        for (int i = 0; i < majorSegments; i++) {
            double theta = Math.TAU * i / majorSegments;
            for (int j = 0; j < minorSegments; j++) {
                double phi = Math.TAU * j / minorSegments;
                double r = major + minor * Math.cos(phi);
                mesh.getPoints().addAll((float) (r * Math.cos(theta)), (float) (minor * Math.sin(phi)),
                        (float) (r * Math.sin(theta)));
            }
        }
        for (int i = 0; i <= majorSegments; i++) {
            for (int j = 0; j <= minorSegments; j++) {
                mesh.getTexCoords().addAll((float) i / majorSegments, (float) j / minorSegments);
            }
        }
        for (int i = 0; i < majorSegments; i++) {
            for (int j = 0; j < minorSegments; j++) {
                int a = i * minorSegments + j;
                int b = ((i + 1) % majorSegments) * minorSegments + j;
                int c = i * minorSegments + (j + 1) % minorSegments;
                int d = ((i + 1) % majorSegments) * minorSegments + (j + 1) % minorSegments;
                int ta = i * (minorSegments + 1) + j;
                int tb = (i + 1) * (minorSegments + 1) + j;
                int tc = ta + 1;
                int td = tb + 1;
                mesh.getFaces().addAll(a, ta, c, tc, b, tb);
                mesh.getFaces().addAll(b, tb, c, tc, d, td);
            }
        }
        return mesh;
    }

    /** A sphere whose radius wobbles, for friendly cartoon asteroids. */
    static TriangleMesh rock(long seed, int detail) {
        var random = new SplittableRandom(seed);
        double[] bumps = new double[6];
        for (int i = 0; i < bumps.length; i++) {
            bumps[i] = random.nextDouble(0, Math.TAU);
        }
        int rings = detail;
        int segments = detail * 2;
        double[] profile = new double[(rings + 1) * 2];
        var mesh = new TriangleMesh();
        for (int i = 0; i <= rings; i++) {
            double lat = Math.PI * i / rings - Math.PI / 2;
            for (int j = 0; j < segments; j++) {
                double lon = Math.TAU * j / segments;
                double wobble = 1 + .16 * Math.sin(lon * 2 + bumps[0]) * Math.cos(lat * 3 + bumps[1])
                        + .10 * Math.sin(lon * 3 + bumps[2] + lat * 2) + .07 * Math.cos(lon * 5 + bumps[3]);
                double r = Math.cos(lat) * wobble;
                if (i == 0 || i == rings) {
                    r = .0005;
                }
                mesh.getPoints().addAll((float) (Math.cos(lon) * r), (float) (Math.sin(lat) * wobble * .85),
                        (float) (Math.sin(lon) * r));
            }
            for (int j = 0; j <= segments; j++) {
                mesh.getTexCoords().addAll((float) j / segments, (float) i / rings);
            }
        }
        for (int i = 0; i < rings; i++) {
            for (int j = 0; j < segments; j++) {
                int a = i * segments + j;
                int b = i * segments + (j + 1) % segments;
                int c = (i + 1) * segments + j;
                int d = (i + 1) * segments + (j + 1) % segments;
                int ta = i * (segments + 1) + j;
                int tb = ta + 1;
                int tc = (i + 1) * (segments + 1) + j;
                int td = tc + 1;
                mesh.getFaces().addAll(a, ta, b, tb, c, tc);
                mesh.getFaces().addAll(b, tb, d, td, c, tc);
            }
        }
        return mesh;
    }

    /** A flat rectangle facing -Z (towards a default camera), with the whole texture mapped upright. */
    static TriangleMesh quad(double width, double height) {
        var mesh = new TriangleMesh();
        float w = (float) (width / 2);
        float h = (float) (height / 2);
        mesh.getPoints().addAll(-w, -h, 0, w, -h, 0, w, h, 0, -w, h, 0);
        mesh.getTexCoords().addAll(0, 0, 1, 0, 1, 1, 0, 1);
        mesh.getFaces().addAll(0, 0, 1, 1, 2, 2, 0, 0, 2, 2, 3, 3);
        return mesh;
    }

    static MeshView view(TriangleMesh mesh, PhongMaterial material) {
        var view = new MeshView(mesh);
        view.setMaterial(material);
        return view;
    }

    /** Turns a node whose axis is +Y so that its axis points along the given direction. */
    static <N extends Node> N orient(N node, double nx, double ny, double nz) {
        double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
        nx /= length;
        ny /= length;
        nz /= length;
        double angle = Math.toDegrees(Math.acos(Math.clamp(ny, -1, 1)));
        if (angle < 1e-4) {
            return node;
        }
        Point3D axis = Math.abs(angle - 180) < 1e-4 ? Rotate.X_AXIS : new Point3D(nz, 0, -nx);
        node.getTransforms().add(new Rotate(angle, axis));
        return node;
    }

    static <N extends Node> N at(N node, double x, double y, double z) {
        node.setTranslateX(x);
        node.setTranslateY(y);
        node.setTranslateZ(z);
        return node;
    }
}
