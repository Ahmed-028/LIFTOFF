package com.liftoff.render3d;

import com.liftoff.model.ModuleSlot;
import javafx.animation.Timeline;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import static com.liftoff.model.ModuleSlot.*;

/** Keeps stable attachment groups and moves them when the airframe or bay changes. */
final class ShipAssembly {
    private static final Bounds DEFAULT_HULL = new BoundingBox(-1.6, -.7, -4, 3.2, 1.5, 8);
    private final Group root = new Group();
    private final ModelLibrary models;
    private final Map<ModuleSlot, Mount> mounts = new EnumMap<>(ModuleSlot.class);
    private final CargoBayDoors doors = new CargoBayDoors();
    private final PhongMaterial dropPaint = ModelLibrary.lit("#8CFFFF", "#20C6D8");
    private ModuleSlot highlight;
    private boolean bayOpen;
    private boolean calmMode;

    ShipAssembly(ModelLibrary models) {
        this.models = models;
        for (var slot : ModuleSlot.values()) {
            var mount = new Mount();
            mounts.put(slot, mount);
            root.getChildren().add(mount.anchor);
        }
        mounts.get(POWER).target.setTranslateX(-1.7);
        updatePayloadVisibility();
        updateAnchors();
    }

    Group node() {
        return root;
    }

    Group anchor(ModuleSlot slot) {
        return mounts.get(slot).anchor;
    }

    Node partNode(ModuleSlot slot) {
        return mounts.get(slot).part;
    }

    CargoBayDoors doors() {
        return doors;
    }

    Node pickNode(ModuleSlot slot) {
        Mount mount = mounts.get(slot);
        return mount.content;
    }

    boolean showPart(ModuleSlot slot, String modelId) {
        Mount mount = mounts.get(slot);
        if (Objects.equals(mount.modelId, modelId)) {
            return false;
        }
        Group replacement = modelId == null ? null : models.create(modelId);
        stopSnap(mount);
        mount.modelId = modelId;
        mount.part = replacement;
        mount.content.getChildren().setAll(replacement == null ? mount.target : replacement);
        if (slot == CARGO_BAY) {
            doors.fit(bounds(CARGO_BAY));
            if (replacement != null) {
                replacement.getChildren().add(doors.node());
            }
        }
        updateAnchors();
        return true;
    }

    void animate(ModuleSlot slot) {
        Mount mount = mounts.get(slot);
        if (!calmMode && mount.part != null && mount.anchor.isVisible()) {
            mount.snap = SnapAnimation.play(mount.part);
            mount.snap.setOnFinished(event -> mount.snap = null);
        }
    }

    void setCalmMode(boolean calmMode) {
        this.calmMode = calmMode;
        if (calmMode) {
            mounts.values().forEach(ShipAssembly::stopSnap);
            doors.settle(this::updatePayloadVisibility);
        }
    }

    void setCargoBayOpen(boolean open, boolean animate) {
        if (open == bayOpen) {
            return;
        }
        bayOpen = open;
        if (open) {
            updatePayloadVisibility();
        }
        doors.setOpen(open, animate && !calmMode && partNode(CARGO_BAY) != null,
                this::updatePayloadVisibility);
    }

    boolean isCalmMode() {
        return calmMode;
    }

    boolean isCargoBayOpen() {
        return bayOpen;
    }

    boolean isAnimating() {
        return doors.isAnimating() || mounts.values().stream().anyMatch(mount -> mount.snap != null);
    }

    private void updatePayloadVisibility() {
        anchor(SATELLITE).setVisible(bayOpen);
    }

    void setDropHighlight(ModuleSlot slot) {
        if (slot == highlight) {
            return;
        }
        if (highlight != null) {
            mounts.get(highlight).outline.setVisible(false);
        }
        highlight = slot;
        if (slot != null) {
            mounts.get(slot).showOutline();
        }
    }

    private void updateAnchors() {
        Bounds hull = bounds(AIRFRAME);
        if (hull == null) {
            hull = DEFAULT_HULL;
        }
        double front = DEFAULT_HULL.getMinZ();
        double length = hull.getDepth();
        double cargoZ = front + length * .58;

        place(AIRFRAME, 0, 0, front - hull.getMinZ());
        place(THRUSTERS, 0, .18, front + length);
        place(POWER, 0, .32, front + length * .39);
        place(CARGO_BAY, 0, -.70, cargoZ);
        place(SATELLITE, 0, -1.05, cargoZ);
    }

    private Bounds bounds(ModuleSlot slot) {
        Node part = partNode(slot);
        return part == null ? null : part.getBoundsInLocal();
    }

    private void place(ModuleSlot slot, double x, double y, double z) {
        var anchor = anchor(slot);
        anchor.setTranslateX(x);
        anchor.setTranslateY(y);
        anchor.setTranslateZ(z);
    }

    private static void stopSnap(Mount mount) {
        if (mount.snap != null) {
            SnapAnimation.finish(mount.part, mount.snap);
            mount.snap = null;
        }
    }

    private final class Mount {
        final Box target = new Box(.54, .24, .54);
        final Group content = new Group(target);
        final Box outline = new Box();
        final Group anchor = new Group(content, outline);
        String modelId;
        Group part;
        Timeline snap;

        Mount() {
            target.setMaterial(ModelLibrary.lit("#237A9F", "#0D3348"));
            outline.setDrawMode(DrawMode.LINE);
            outline.setCullFace(CullFace.BACK);
            outline.setMaterial(dropPaint);
            outline.setMouseTransparent(true);
            outline.setVisible(false);
            content.boundsInLocalProperty().addListener((property, previous, bounds) -> {
                if (outline.isVisible()) {
                    fitOutline(bounds);
                }
            });
        }

        void showOutline() {
            fitOutline(content.getBoundsInLocal());
            outline.setVisible(true);
        }

        private void fitOutline(Bounds bounds) {
            outline.setWidth(bounds.getWidth() + .12);
            outline.setHeight(bounds.getHeight() + .12);
            outline.setDepth(bounds.getDepth() + .12);
            outline.setTranslateX(bounds.getCenterX());
            outline.setTranslateY(bounds.getCenterY());
            outline.setTranslateZ(bounds.getCenterZ());
        }
    }
}
