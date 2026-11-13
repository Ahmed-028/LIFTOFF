package com.liftoff.engine;

/**
 * What the ship carries and pays for beyond its five parts, chosen on the Supplies step.
 *
 * @param instrumentMassKg   mass of the chosen instruments
 * @param instrumentPowerW   average power the instruments draw
 * @param suppliesKg         crew supplies packed
 * @param requiredSuppliesKg crew supplies the trip needs; 0 for a robotic mission
 * @param otherCostMusd      everything bought that is not a ship part: instruments, stations, rocket
 */
public record BuildLoad(double instrumentMassKg, double instrumentPowerW, double suppliesKg,
                        double requiredSuppliesKg, double otherCostMusd) {

    public static final BuildLoad NONE = new BuildLoad(0, 0, 0, 0, 0);

    public BuildLoad {
        requireAmount(instrumentMassKg, "instrumentMassKg");
        requireAmount(instrumentPowerW, "instrumentPowerW");
        requireAmount(suppliesKg, "suppliesKg");
        requireAmount(requiredSuppliesKg, "requiredSuppliesKg");
        requireAmount(otherCostMusd, "otherCostMusd");
    }

    private static void requireAmount(double value, String field) {
        if (!(value >= 0) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(field + " must be zero or more, was " + value);
        }
    }
}
