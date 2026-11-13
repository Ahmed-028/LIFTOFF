package com.liftoff.engine.sim;

/**
 * Flight inputs for explicit simulation scenarios. The default simulator uses the selected bus's recorder.
 * Crew supplies and power source can be supplied once the builder exposes those choices.
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
