package com.liftoff.render3d;

import com.liftoff.model.ModuleSlot;
import javafx.scene.Node;

public interface ShipView {
    Node node();


    void showPart(ModuleSlot slot, String modelId);

    void setCargoBayOpen(boolean open);


    void setDropHighlight(ModuleSlot slot);


    ModuleSlot slotAt(double sceneX, double sceneY);
}
