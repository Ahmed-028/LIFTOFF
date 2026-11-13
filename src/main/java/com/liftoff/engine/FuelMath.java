package com.liftoff.engine;

/**
 * The rocket equation (Tsiolkovsky), in the two directions the game needs:
 *
 * <pre>
 * propellant = dry mass x ( e^( delta-v / (Isp x g0) ) - 1 )
 * delta-v    = Isp x g0 x ln( (dry mass + propellant) / dry mass )
 * </pre>
 *
 * Isp (specific impulse, seconds) is a property of the engine; delta-v (m/s) is the speed change
 * the trip needs. Ideal, single-burn form: no gravity or drag losses (doc 05, section 7).
 */
public final class FuelMath {

    /** Standard acceleration of gravity, m/s^2. A defined constant (exact), used to turn Isp into exhaust speed. */
    public static final double STANDARD_GRAVITY_MS2 = 9.80665;

    private FuelMath() {
    }

    /** Propellant, in kg, needed to change the speed of a ship of the given dry mass by deltaVMs. */
    public static double propellantKg(double dryMassKg, double deltaVMs, double specificImpulseS) {
        requirePositive(dryMassKg, "Dry mass");
        requirePositive(specificImpulseS, "Specific impulse");
        requireNotNegative(deltaVMs, "Delta-v");
        double exhaustSpeedMs = specificImpulseS * STANDARD_GRAVITY_MS2;
        return dryMassKg * Math.expm1(deltaVMs / exhaustSpeedMs);
    }

    /** Speed change, in m/s, that the given propellant can give a ship of the given dry mass. */
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
