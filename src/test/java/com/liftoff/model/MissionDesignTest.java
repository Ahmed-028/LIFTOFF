package com.liftoff.model;

import com.liftoff.TestParts;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionDesignTest {

    private final Instrument camera = TestParts.instrument("camera", 50, 80, 1000, 25, 18);
    private final Instrument radar = TestParts.instrument("radar", 40, 100, 500, 20, 36);
    private final GroundStation north = TestParts.station("north", 78, 0.5);

    @Test
    void emptyDesignHasNoPartsChosen() {
        MissionDesign design = MissionDesign.empty();

        assertNull(design.objective());
        assertNull(design.bus());
        assertNull(design.orbit());
        assertNull(design.launchVehicle());
        assertTrue(design.instruments().isEmpty());
        assertTrue(design.stations().isEmpty());
        assertEquals(0, design.scienceMonths());
    }

    @Test
    void choosingAPartReturnsANewDesignAndLeavesTheOldOneAlone() {
        MissionDesign before = MissionDesign.empty();
        Bus bus = TestParts.bus("bus", 400, 200, 10, 60, 30);

        MissionDesign after = before.withBus(bus);

        assertSame(bus, after.bus());
        assertNull(before.bus());
    }

    @Test
    void singlePartsCanAllBeSet() {
        Objective objective = TestParts.objective("goal", 300, 80, 24);
        Orbit orbit = TestParts.orbit("orbit", 705, 90, LaunchClass.POLAR);
        LaunchVehicle rocket = TestParts.rocket("rocket", Map.of(LaunchClass.POLAR, 500.0), 100);

        MissionDesign design = MissionDesign.empty()
                .withObjective(objective)
                .withOrbit(orbit)
                .withLaunchVehicle(rocket)
                .withScienceMonths(24);

        assertSame(objective, design.objective());
        assertSame(orbit, design.orbit());
        assertSame(rocket, design.launchVehicle());
        assertEquals(24, design.scienceMonths());
    }

    @Test
    void instrumentsAreAddedInOrder() {
        MissionDesign design = MissionDesign.empty().plusInstrument(camera).plusInstrument(radar);

        assertEquals(2, design.instruments().size());
        assertSame(camera, design.instruments().get(0));
        assertSame(radar, design.instruments().get(1));
        assertTrue(design.hasInstrument("radar"));
    }

    @Test
    void theSameInstrumentIsNotAddedTwice() {
        MissionDesign design = MissionDesign.empty().plusInstrument(camera).plusInstrument(camera);

        assertEquals(1, design.instruments().size());
    }

    @Test
    void anInstrumentCanBeRemovedById() {
        MissionDesign design = MissionDesign.empty().plusInstrument(camera).plusInstrument(radar)
                .minusInstrument("camera");

        assertEquals(1, design.instruments().size());
        assertFalse(design.hasInstrument("camera"));
    }

    @Test
    void stationsCanBeAddedAndRemoved() {
        MissionDesign withStation = MissionDesign.empty().plusStation(north).plusStation(north);

        assertEquals(1, withStation.stations().size());
        assertTrue(withStation.hasStation("north"));
        assertTrue(withStation.minusStation("north").stations().isEmpty());
    }

    @Test
    void thePartListsCannotBeChangedFromOutside() {
        MissionDesign design = MissionDesign.empty().plusInstrument(camera);

        assertThrows(UnsupportedOperationException.class, () -> design.instruments().add(radar));
    }
}
