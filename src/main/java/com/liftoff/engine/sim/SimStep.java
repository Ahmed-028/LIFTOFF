package com.liftoff.engine.sim;


public record SimStep(int index, double dataProducedGbit, double dataSentGbit, double dataLostGbit,
                      double storageUsedGbit, double powerShortfallW, double suppliesLeftKg) {
}
