package com.liftoff.model;

import java.util.Map;
import java.util.OptionalDouble;

// Lift limits are published per orbit class.
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


    public OptionalDouble capacityFor(LaunchClass launchClass) {
        Double capacity = capacityKg.get(launchClass);
        return capacity == null ? OptionalDouble.empty() : OptionalDouble.of(capacity);
    }
}
