package com.liftoff.model;

// A null resolution accepts any level of detail.
public record Requirement(MeasurementType type, Double maxResolutionM) {
}
