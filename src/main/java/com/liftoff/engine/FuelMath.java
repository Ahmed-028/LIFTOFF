package com.liftoff.engine;

// Ideal single-burn Tsiolkovsky equation; ignores gravity and drag losses (doc 05, section 7).
// propellant = dry mass * (e^(delta-v / (Isp * g0)) - 1)
// delta-v = Isp * g0 * ln((dry mass + propellant) / dry mass)
public final class FuelMath {


    public static final double STANDARD_GRAVITY_MS2 = 9.80665;

    private FuelMath() {
    }


    public static double propellantKg(double dryMassKg, double deltaVMs, double specificImpulseS) {
        requirePositive(dryMassKg, "Dry mass");
        requirePositive(specificImpulseS, "Specific impulse");
        requireNotNegative(deltaVMs, "Delta-v");
        double exhaustSpeedMs = specificImpulseS * STANDARD_GRAVITY_MS2;
        return dryMassKg * Math.expm1(deltaVMs / exhaustSpeedMs);
    }


    public static double deltaVMs(double dryMassKg, double propellantKg, double specificImpulseS) {
        requirePositive(dryMassKg, "Dry mass");
        requirePositive(specificImpulseS, "Specific impulse");
        requireNotNegative(propellantKg, "Propellant");
        double exhaustSpeedMs = specificImpulseS * STANDARD_GRAVITY_MS2;
        return exhaustSpeedMs * Math.log1p(propellantKg / dryMassKg);
    }

    private static void requirePositive(double value, String name) {
        if (!(value > 0)) {
            throw new IllegalArgumentException(name + " must be above zero: " + value);
        }
    }

    private static void requireNotNegative(double value, String name) {
        if (!(value >= 0)) {
            throw new IllegalArgumentException(name + " cannot be negative: " + value);
        }
    }
}
