package com.liftoff.model;

// A null resolution means the source does not state one.
public record Capability(MeasurementType type, Double resolutionM) {
}
