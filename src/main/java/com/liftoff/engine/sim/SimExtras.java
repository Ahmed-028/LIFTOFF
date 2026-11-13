package com.liftoff.engine.sim;

/**
 * Inputs the simulator needs that the model does not carry yet.
 * TODO(Member 1/2): replace with Bus.storageGbit (data-sources "still to source" 3), the chosen
 * supplies of the Supplies step, and the kind of the POWER part, once those exist.
 *
 * @param storageGbit  size of the data recorder; infinite means "no figure yet, not limited"
 * @param suppliesKg   crew supplies loaded at launch
 * @param solarPowered true when the power comes from solar arrays (dust matters), false for a nuclear source
 */
public record SimExtras(double storageGbit, double suppliesKg, boolean solarPowered) {

    /** No storage limit (no sourced figure is ever assumed), no supplies, solar power. */
    public static SimExtras none() {
        return new SimExtras(Double.POSITIVE_INFINITY, 0, true);
    }
}
