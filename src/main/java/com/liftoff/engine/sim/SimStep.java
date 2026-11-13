package com.liftoff.engine.sim;

/**
 * The state of the flight after one step (a month or a day).
 *
 * @param storageUsedGbit data waiting on board at the end of the step
 * @param powerShortfallW how far the instruments' draw exceeded the supply during the step
 */
public record SimStep(int index, double dataProducedGbit, double dataSentGbit, double dataLostGbit,
                      double storageUsedGbit, double powerShortfallW, double suppliesLeftKg) {
}
