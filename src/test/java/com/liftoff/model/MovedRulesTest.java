package com.liftoff.model;

import com.liftoff.TestParts;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Rules that used to live in the screens and now live with the data they are about. */
class MovedRulesTest {

    @Test
    void aRocketIsOfferedForADestinationOnlyWithAPublishedFigureForIt() {
        LaunchVehicle small = TestParts.rocket("small", Map.of(LaunchClass.POLAR, 200.0), 1);
        LaunchVehicle moonRocket = TestParts.rocket("moon", Map.of(LaunchClass.TRANS_LUNAR, 27_000.0), 1);

        assertTrue(small.offeredFor(Destination.EARTH_ORBIT));
        assertFalse(small.offeredFor(Destination.MOON_SURFACE));
        assertTrue(moonRocket.offeredFor(Destination.MOON_SURFACE));
        assertFalse(moonRocket.offeredFor(Destination.MARS_SURFACE));
        assertFalse(moonRocket.offeredFor(Destination.EARTH_ORBIT));
    }

    @Test
    void eachDestinationUsesItsOwnRocketCapacity() {
        Orbit polar = TestParts.orbit("polar", 700, 98, LaunchClass.POLAR);
        assertEquals(LaunchClass.POLAR, Destination.EARTH_ORBIT.launchClass(polar));
        assertNull(Destination.EARTH_ORBIT.launchClass(null));
        assertEquals(LaunchClass.TRANS_LUNAR, Destination.MOON_SURFACE.launchClass(polar));
        assertEquals(LaunchClass.TRANS_MARS, Destination.MARS_SURFACE.launchClass(null));
    }

    @Test
    void instrumentsForTheGoalComeFirstThenTheBonusThenTheRest() {
        Objective goal = new Objective("goal", "goal", "", "", "", 0, false,
                List.of(new Requirement(MeasurementType.LAND_IMAGING, 30.0)), List.of(MeasurementType.THERMAL_IMAGING),
                100, 60, 12, TestParts.SOURCE);
        Instrument camera = TestParts.instrument("camera", List.of(new Capability(MeasurementType.LAND_IMAGING, 250.0)));
        Instrument heat = TestParts.instrument("heat", List.of(new Capability(MeasurementType.THERMAL_IMAGING, 60.0)));
        Instrument rain = TestParts.instrument("rain", List.of(new Capability(MeasurementType.RAIN_MICROWAVE, null)));

        assertEquals(0, goal.relevanceOf(camera), "measures what the goal needs, however coarsely");
        assertEquals(1, goal.relevanceOf(heat));
        assertEquals(2, goal.relevanceOf(rain));
    }
}
