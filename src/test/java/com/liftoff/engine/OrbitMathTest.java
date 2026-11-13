package com.liftoff.engine;

import com.liftoff.TestParts;
import com.liftoff.model.LaunchClass;
import com.liftoff.model.Orbit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrbitMathTest {

    private static final double MINUTES_PER_DAY = 1440;

    @Test
    void periodAt705KmMatchesThePublishedFigureForAqua() {
        // NASA Earth Science Reference Handbook (2006), Key Aqua Facts: 98.8 minutes.
        assertEquals(98.8, OrbitMath.periodMinutes(705), 0.1);
    }

    @Test
    void periodAt400KmMatchesThePublishedFigureForGpm() {
        // Handbook, Key GPM Core Facts: 92.56 minutes for a 400 km orbit.
        assertEquals(92.56, OrbitMath.periodMinutes(400), 0.05);
    }

    @Test
    void anOrbitReachesLatitudesUpToItsInclination() {
        assertEquals(35.0, OrbitMath.maxLatitudeDeg(35), 1e-9);
    }

    @Test
    void aRetrogradeOrbitReachesOneEightyMinusItsInclination() {
        assertEquals(81.8, OrbitMath.maxLatitudeDeg(98.2), 1e-9);
        assertEquals(86.0, OrbitMath.maxLatitudeDeg(94), 1e-9);
    }

    @Test
    void viewAngleFollowsTheHorizonGeometry() {
        // arccos(6378.137 x cos 5 deg / 7083.137) - 5 deg = 21.23 deg
        assertEquals(21.23, OrbitMath.viewAngleDeg(705, 5), 0.01);
    }

    @Test
    void aStationAtThePoleSeesAPolarOrbiterForItsViewAngleShareOfEveryLap() {
        // Each lap crosses the pole once and stays in view for 2 x viewAngle of 360 degrees.
        Orbit polar = TestParts.orbit("polar", 705, 90, LaunchClass.POLAR);
        double expected = MINUTES_PER_DAY * OrbitMath.viewAngleDeg(705, GameRules.MIN_ELEVATION_DEG) / 180;

        assertEquals(expected, OrbitMath.contactMinutesPerDay(polar, 90), 0.5);
    }

    @Test
    void aStationOnTheEquatorSeesAnEquatorialOrbiterForItsViewAngleShareOfTheEquator() {
        Orbit equatorial = TestParts.orbit("equatorial", 402, 0, LaunchClass.LOW_INCLINATION);
        double expected = MINUTES_PER_DAY * OrbitMath.viewAngleDeg(402, GameRules.MIN_ELEVATION_DEG) / 180;

        assertEquals(expected, OrbitMath.contactMinutesPerDay(equatorial, 0), 0.5);
    }

    @Test
    void aPolarStationNeverSeesATropicalOrbiter() {
        Orbit tropical = TestParts.orbit("tropical", 402, 35, LaunchClass.LOW_INCLINATION);

        assertEquals(0.0, OrbitMath.contactMinutesPerDay(tropical, 78.2), 1e-9);
    }

    @Test
    void aHighLatitudeStationSeesAPolarOrbiterLongerThanAMidLatitudeOne() {
        Orbit sunSynchronous = TestParts.orbit("sso", 705, 98.2, LaunchClass.POLAR);

        double arctic = OrbitMath.contactMinutesPerDay(sunSynchronous, 78.2);
        double midLatitude = OrbitMath.contactMinutesPerDay(sunSynchronous, 37.9);

        assertTrue(midLatitude > 0, "a mid-latitude station still gets some passes");
        assertTrue(arctic > 2 * midLatitude, "arctic " + arctic + " vs mid-latitude " + midLatitude);
    }

    @Test
    void northAndSouthStationsAtTheSameLatitudeGetTheSameContact() {
        Orbit sunSynchronous = TestParts.orbit("sso", 705, 98.2, LaunchClass.POLAR);

        assertEquals(OrbitMath.contactMinutesPerDay(sunSynchronous, 64.9),
                OrbitMath.contactMinutesPerDay(sunSynchronous, -64.9), 0.01);
    }
}
