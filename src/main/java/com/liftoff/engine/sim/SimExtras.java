package com.liftoff.engine.sim;


public record SimExtras(double storageGbit, double suppliesKg, boolean solarPowered) {

    // Do not assume a storage limit without a published value.
    public static SimExtras none() {
        return new SimExtras(Double.POSITIVE_INFINITY, 0, true);
    }
}
