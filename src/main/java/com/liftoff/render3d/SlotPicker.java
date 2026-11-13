package com.liftoff.render3d;

import com.liftoff.model.ModuleSlot;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.SubScene;
import javafx.scene.shape.Box;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

final class SlotPicker {
    private static final double EPSILON = 1e-9;
    private final SubScene viewport;
    private final OrbitCamera camera;
    private final ShipAssembly ship;

    SlotPicker(SubScene viewport, OrbitCamera camera, ShipAssembly ship) {
        this.viewport = viewport;
        this.camera = camera;
        this.ship = ship;
    }

    ModuleSlot slotAt(double sceneX, double sceneY) {
        Point2D cursor = viewport.sceneToLocal(sceneX, sceneY, true);
        if (cursor == null || cursor.getX() < 0 || cursor.getX() >= viewport.getWidth()
                || cursor.getY() < 0 || cursor.getY() >= viewport.getHeight()) {
            return null;
        }
        Point3D origin = camera.origin();
        Point3D through = camera.rayThrough(cursor.getX(), cursor.getY(),
                viewport.getWidth(), viewport.getHeight());
        ModuleSlot closest = null;
        double distance = Double.POSITIVE_INFINITY;
        for (var slot : ModuleSlot.values()) {
            if (ship.anchor(slot).isVisible()) {
                double hit = hit(ship.pickNode(slot), origin, through);
                if (hit < distance) {
                    distance = hit;
                    closest = slot;
                }
            }
        }
        return closest;
    }

    private static double hit(Node node, Point3D origin, Point3D through) {
        if (!node.isVisible() || node.isMouseTransparent()) {
            return Double.POSITIVE_INFINITY;
        }
        Point3D start = node.sceneToLocal(origin);
        Point3D end = node.sceneToLocal(through);
        if (start == null || end == null) {
            return Double.POSITIVE_INFINITY;
        }
        Point3D direction = end.subtract(start);
        double bounds = hitBounds(node.getBoundsInLocal(), start, direction);
        if (Double.isInfinite(bounds) || node instanceof Box) {
            return bounds;
        }
        if (node instanceof MeshView view && view.getMesh() instanceof TriangleMesh mesh) {
            return hitMesh(mesh, start, direction);
        }
        if (node instanceof Parent parent) {
            double nearest = Double.POSITIVE_INFINITY;
            for (Node child : parent.getChildrenUnmodifiable()) {
                nearest = Math.min(nearest, hit(child, origin, through));
            }
            return nearest;
        }
        return Double.POSITIVE_INFINITY;
    }

    private static double hitBounds(Bounds b, Point3D origin, Point3D direction) {
        double near = 0;
        double far = Double.POSITIVE_INFINITY;
        double[] from = {origin.getX(), origin.getY(), origin.getZ()};
        double[] along = {direction.getX(), direction.getY(), direction.getZ()};
        double[] min = {b.getMinX(), b.getMinY(), b.getMinZ()};
        double[] max = {b.getMaxX(), b.getMaxY(), b.getMaxZ()};
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(along[axis]) < EPSILON) {
                if (from[axis] < min[axis] || from[axis] > max[axis]) {
                    return Double.POSITIVE_INFINITY;
                }
                continue;
            }
            double a = (min[axis] - from[axis]) / along[axis];
            double z = (max[axis] - from[axis]) / along[axis];
            near = Math.max(near, Math.min(a, z));
            far = Math.min(far, Math.max(a, z));
            if (near > far) {
                return Double.POSITIVE_INFINITY;
            }
        }
        return near;
    }

    private static double hitMesh(TriangleMesh mesh, Point3D origin, Point3D direction) {
        double nearest = Double.POSITIVE_INFINITY;
        int faceSize = mesh.getFaceElementSize();
        int stride = faceSize / 3;
        for (int face = 0; face < mesh.getFaces().size(); face += faceSize) {
            Point3D a = vertex(mesh, face);
            Point3D b = vertex(mesh, face + stride);
            Point3D c = vertex(mesh, face + 2 * stride);
            nearest = Math.min(nearest, hitTriangle(a, b, c, origin, direction));
        }
        return nearest;
    }

    private static Point3D vertex(TriangleMesh mesh, int faceIndex) {
        int vertex = mesh.getFaces().get(faceIndex) * mesh.getPointElementSize();
        var points = mesh.getPoints();
        return new Point3D(points.get(vertex), points.get(vertex + 1), points.get(vertex + 2));
    }

    private static double hitTriangle(Point3D a, Point3D b, Point3D c, Point3D origin, Point3D direction) {
        Point3D edge = b.subtract(a);
        Point3D otherEdge = c.subtract(a);
        Point3D p = direction.crossProduct(otherEdge);
        double determinant = edge.dotProduct(p);
        if (determinant < EPSILON) {
            return Double.POSITIVE_INFINITY;
        }
        Point3D fromA = origin.subtract(a);
        double u = fromA.dotProduct(p) / determinant;
        Point3D q = fromA.crossProduct(edge);
        double v = direction.dotProduct(q) / determinant;
        double distance = otherEdge.dotProduct(q) / determinant;
        return u >= 0 && v >= 0 && u + v <= 1 && distance >= 0 ? distance : Double.POSITIVE_INFINITY;
    }
}
