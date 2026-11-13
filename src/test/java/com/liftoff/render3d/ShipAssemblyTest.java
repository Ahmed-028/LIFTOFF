package com.liftoff.render3d;

import com.liftoff.model.ModuleSlot;
import org.junit.jupiter.api.Test;

import static com.liftoff.model.ModuleSlot.*;
import static org.junit.jupiter.api.Assertions.*;

class ShipAssemblyTest {
    @Test
    void dropOutlineTracksPartChangesAndClearsWithoutReplacingTheMount() {
        var ship = new ShipAssembly(new ModelLibrary());
        ship.showPart(POWER, "body-solar");
        ship.setDropHighlight(POWER);
        var mount = ship.anchor(POWER);
        var outline = (javafx.scene.shape.Box) mount.getChildren().getLast();
        double before = outline.getWidth();
        assertTrue(outline.isVisible());
        assertTrue(outline.isMouseTransparent());

        ship.showPart(POWER, "deployable-solar");
        assertSame(outline, mount.getChildren().getLast());
        assertTrue(outline.getWidth() > before);
        ship.setDropHighlight(null);
        assertFalse(outline.isVisible());
    }

    @Test
    void longerAirframeMovesThrustersWithoutReplacingOtherParts() {
        var ship = new ShipAssembly(new ModelLibrary());
        ship.showPart(AIRFRAME, "x-37b");
        ship.showPart(THRUSTERS, "shuttle-oms");
        ship.showPart(POWER, "body-solar");
        ship.showPart(CARGO_BAY, "bay-3u");
        ship.showPart(SATELLITE, "cubesat-1u");

        var nodes = new java.util.EnumMap<ModuleSlot, javafx.scene.Node>(ModuleSlot.class);
        for (var slot : ModuleSlot.values()) {
            nodes.put(slot, ship.partNode(slot));
        }
        double oldThrusterZ = ship.anchor(THRUSTERS).getTranslateZ();
        ship.showPart(AIRFRAME, "shuttle-orbiter");

        assertNotSame(nodes.get(AIRFRAME), ship.partNode(AIRFRAME));
        for (var slot : java.util.List.of(THRUSTERS, POWER, CARGO_BAY, SATELLITE)) {
            assertSame(nodes.get(slot), ship.partNode(slot));
        }
        assertTrue(ship.anchor(THRUSTERS).getTranslateZ() > oldThrusterZ);
    }

    @Test
    void repeatedSelectionsKeepThePartAndAMissingModelShowsABoxInsteadOfFailing() {
        var ship = new ShipAssembly(new ModelLibrary());
        assertTrue(ship.showPart(POWER, "body-solar"));
        var power = ship.partNode(POWER);
        assertFalse(ship.showPart(POWER, "body-solar"));
        assertSame(power, ship.partNode(POWER));
        assertThrows(IllegalArgumentException.class, () -> ship.showPart(POWER, "not a valid id"));
        assertSame(power, ship.partNode(POWER), "a refused id leaves the part in place");

        assertTrue(ship.showPart(POWER, "unknown"));
        assertNotNull(ship.partNode(POWER), "the game carries on with a labelled box");
        assertTrue(ModelLibrary.failed("unknown"));
        assertTrue(ship.showPart(POWER, null));
        assertNull(ship.partNode(POWER));
    }

    @Test
    void openBaySurvivesReplacementAndControlsPayloadVisibility() {
        var ship = new ShipAssembly(new ModelLibrary());
        ship.showPart(CARGO_BAY, "bay-3u");
        ship.showPart(SATELLITE, "cubesat-1u");
        assertFalse(ship.anchor(SATELLITE).isVisible());

        ship.setCargoBayOpen(true, false);
        assertTrue(ship.anchor(SATELLITE).isVisible());
        assertNotEquals(0, ship.doors().leftAngle());
        double narrowHinge = ship.doors().halfSpan();

        ship.showPart(CARGO_BAY, "bay-12u");
        assertTrue(ship.anchor(SATELLITE).isVisible());
        assertTrue(ship.doors().halfSpan() > narrowHinge);
        assertNotEquals(0, ship.doors().leftAngle());

        ship.setCargoBayOpen(false, false);
        assertFalse(ship.anchor(SATELLITE).isVisible());
        assertEquals(0, ship.doors().leftAngle(), 1e-9);
    }
}
