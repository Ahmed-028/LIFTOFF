package com.liftoff.model;

import java.util.List;
import java.util.Objects;


public record ShipPart(
        String id, ModuleSlot slot, String name, String kidName, String description,
        String basedOn, List<String> missions,
        double massKg, double powerSupplyW, double powerDrawW,
        double payloadMassKg, double bayLengthM, double bayDiameterM,
        double lengthM, double diameterM,
        double thrustN, double specificImpulseS, double propellantCapacityKg,
        double dataGbitPerDay, double costMusd, ValueBasis costBasis,
        String modelId, SourceRef source) {

    public ShipPart {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(slot, "slot");
        Objects.requireNonNull(source, "source");
        missions = List.copyOf(missions);

        requireAmount(massKg, "massKg", id);
        requireAmount(powerSupplyW, "powerSupplyW", id);
        requireAmount(powerDrawW, "powerDrawW", id);
        requireAmount(payloadMassKg, "payloadMassKg", id);
        requireAmount(bayLengthM, "bayLengthM", id);
        requireAmount(bayDiameterM, "bayDiameterM", id);
        requireAmount(lengthM, "lengthM", id);
        requireAmount(diameterM, "diameterM", id);
        requireAmount(thrustN, "thrustN", id);
        requireAmount(specificImpulseS, "specificImpulseS", id);
        requireAmount(propellantCapacityKg, "propellantCapacityKg", id);
        requireAmount(dataGbitPerDay, "dataGbitPerDay", id);
        requireAmount(costMusd, "costMusd", id);

        // The rocket equation requires specific impulse.
        if (slot == ModuleSlot.THRUSTERS && specificImpulseS <= 0) {
            throw new IllegalArgumentException("Thruster " + id + " needs a specific impulse above zero");
        }
    }


    private static void requireAmount(double value, String field, String id) {
        if (!(value >= 0) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("Part " + id + ": " + field + " must be zero or more, was " + value);
        }
    }


    public boolean offeredIn(String missionId) {
        return missions.contains(missionId);
    }
}
