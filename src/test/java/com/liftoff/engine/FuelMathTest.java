package com.liftoff.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FuelMathTest {

    private static final double TINY = 1e-6;

    @Test
    void standardGravityIsTheDefinedConstant() {
        // g0 = 9.80665 m/s^2 exactly (defined constant).
        assertEquals(9.80665, FuelMath.STANDARD_GRAVITY_MS2);
    }

    @Test
    void speedChangeOfIspTimesG0TimesLn2NeedsFuelEqualToDryMass() {
        // The check in doc 06: e^(ln 2) - 1 = 1, so propellant = dry mass.
        double isp = 300;
        double deltaV = isp * 9.80665 * Math.log(2);
        assertEquals(1000, FuelMath.propellantKg(1000, deltaV, isp), TINY);
    }

    @Test
    void noSpeedChangeNeedsNoFuel() {
        assertEquals(0, FuelMath.propellantKg(1000, 0, 300));
    }

    @Test
    void emptyTanksGiveNoSpeedChange() {
        assertEquals(0, FuelMath.deltaVMs(1000, 0, 300));
    }

    @Test
    void theTwoFunctionsUndoEachOther() {
        double propellant = FuelMath.propellantKg(2350, 1000, 300);
        assertEquals(1000, FuelMath.deltaVMs(2350, propellant, 300), TINY);
    }

    @Test
    void fuelGrowsFasterThanTheSpeedChange() {

        double once = FuelMath.propellantKg(1000, 2000, 300);
        double twice = FuelMath.propellantKg(1000, 4000, 300);
        assertTrue(twice > 2 * once);
    }

    @Test
    void aBetterEngineNeedsLessFuel() {
        assertTrue(FuelMath.propellantKg(1000, 2000, 450) < FuelMath.propellantKg(1000, 2000, 300));
    }

    @Test
    void fuelScalesWithDryMass() {
        assertEquals(2 * FuelMath.propellantKg(1000, 2000, 300), FuelMath.propellantKg(2000, 2000, 300), TINY);
    }

    @Test
    void rejectsImpossibleInputs() {
        assertThrows(IllegalArgumentException.class, () -> FuelMath.propellantKg(0, 1000, 300));
        assertThrows(IllegalArgumentException.class, () -> FuelMath.propellantKg(-5, 1000, 300));
        assertThrows(IllegalArgumentException.class, () -> FuelMath.propellantKg(1000, 1000, 0));
        assertThrows(IllegalArgumentException.class, () -> FuelMath.propellantKg(1000, -1, 300));
        assertThrows(IllegalArgumentException.class, () -> FuelMath.deltaVMs(0, 100, 300));
        assertThrows(IllegalArgumentException.class, () -> FuelMath.deltaVMs(1000, -1, 300));
        assertThrows(IllegalArgumentException.class, () -> FuelMath.deltaVMs(1000, 100, 0));
    }
}
