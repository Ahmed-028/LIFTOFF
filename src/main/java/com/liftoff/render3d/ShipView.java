package com.liftoff.render3d;

import com.liftoff.model.ModuleSlot;
import javafx.scene.Node;

public interface ShipView {
    Node node();

    /** Swaps the slot's visual model without moving the camera. Null empties the slot. */
    void showPart(ModuleSlot slot, String modelId);

    void setCargoBayOpen(boolean open);

    /** Highlights a drop target. Null clears the highlight. */
    void setDropHighlight(ModuleSlot slot);

    /** Returns the frontmost visible slot at a point in the embedding scene, or null. */
    ModuleSlot slotAt(double sceneX, double sceneY);

    /** Called when the screen closes: stops animations and lets go of the scene. */
    default void dispose() {
    }
}
