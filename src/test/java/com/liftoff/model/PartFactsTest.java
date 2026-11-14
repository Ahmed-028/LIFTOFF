package com.liftoff.model;

import com.liftoff.TestParts;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartFactsTest {

    @Test
    void anInstrumentsDailyDataIsItsAverageRateOverOneDay() {

        Instrument imager = TestParts.instrument("imager", 229, 162.5, 6200, 114.5, 30);

        assertEquals(535.68, imager.dataGbitPerDay(), 1e-9);
    }

    @Test
    void aRocketGivesItsCapacityForAnOrbitClassItHasAFigureFor() {
        LaunchVehicle rocket = TestParts.rocket("rocket", Map.of(LaunchClass.POLAR, 8080.0), 150);

        assertEquals(8080.0, rocket.capacityFor(LaunchClass.POLAR).orElseThrow(), 1e-9);
    }

    @Test
    void aRocketHasNoCapacityForAnOrbitClassWithoutAPublishedFigure() {
        LaunchVehicle rocket = TestParts.rocket("rocket", Map.of(LaunchClass.LOW_INCLINATION, 450.0), 50);

        assertTrue(rocket.capacityFor(LaunchClass.POLAR).isEmpty());
    }
}
