package com.liftoff.model;

import com.liftoff.TestParts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The v2 additions to MissionDesign: the mission and the ship build. */
class MissionDesignShipTest {

    private static final ShipPart PLANE = TestParts.airframe("plane", 1000, 500, 2000, 50, 100);
    private static final ShipPart SOLAR = TestParts.power("solar", 50, 200, 20);

    @Test
    void anEmptyDesignHasNoMissionAndAnEmptyShip() {
        MissionDesign design = MissionDesign.empty();
        assertNull(design.mission());
        assertTrue(design.build().parts().isEmpty());
    }

    @Test
    void withMissionKeepsEverythingElse() {
        Mission mission = TestParts.mission("trip", 200, 1000);
        MissionDesign design = MissionDesign.empty().withPart(PLANE).withScienceMonths(12).withMission(mission);
        assertSame(mission, design.mission());
        assertSame(PLANE, design.build().part(ModuleSlot.AIRFRAME).orElseThrow());
        assertEquals(12, design.scienceMonths());
    }

    @Test
    void withPartSwapsOneSlotAndKeepsTheOthers() {
        ShipPart fuelCell = TestParts.power("fuel-cell", 100, 1000, 20);
        MissionDesign design = MissionDesign.empty().withPart(PLANE).withPart(SOLAR).withPart(fuelCell);
        assertSame(PLANE, design.build().part(ModuleSlot.AIRFRAME).orElseThrow());
        assertSame(fuelCell, design.build().part(ModuleSlot.POWER).orElseThrow());
    }

    @Test
    void withBuildReplacesTheWholeShip() {
        ShipBuild build = ShipBuild.empty().with(PLANE).with(SOLAR);
        MissionDesign design = MissionDesign.empty().withBuild(build);
        assertSame(build, design.build());
    }

    @Test
    void changingTheShipLeavesTheOldDesignAlone() {
        MissionDesign before = MissionDesign.empty();
        before.withPart(PLANE);
        assertTrue(before.build().parts().isEmpty());
    }

    @Test
    void theShipIsNeverNull() {
        assertThrows(NullPointerException.class, () -> MissionDesign.empty().withBuild(null));
    }

    @Test
    void theOldPartsStillWorkBesideTheShip() {
        // Bus and objective stay until the session and the loader move to missions and ship parts.
        Bus bus = TestParts.bus("bus", 400, 100, 100, 60, 24);
        MissionDesign design = MissionDesign.empty().withBus(bus).withPart(PLANE);
        assertSame(bus, design.bus());
        assertSame(PLANE, design.build().part(ModuleSlot.AIRFRAME).orElseThrow());
    }
}
