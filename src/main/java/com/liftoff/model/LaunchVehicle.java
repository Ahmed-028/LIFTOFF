package com.liftoff.model;

import java.util.Map;
import java.util.OptionalDouble;

/** A rocket, with its published lift capacity per class of orbit. */
public record LaunchVehicle(
        String id,
        String name,
        String kidName,
        String description,
        Map<LaunchClass, Double> capacityKg,
        double costMusd,
        ValueBasis costBasis,
        SourceRef source) {

    public LaunchVehicle {
        capacityKg = Map.copyOf(capacityKg);
    }

    /** Lift capacity to the given class of orbit; empty when the catalog has no published figure. */
    public OptionalDouble capacityFor(LaunchClass launchClass) {
        Double capacity = capacityKg.get(launchClass);
        return capacity == null ? OptionalDouble.empty() : OptionalDouble.of(capacity);
    }
}
