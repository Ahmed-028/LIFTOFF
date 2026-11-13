package com.liftoff.model;

/**
 * A measurement an objective must have.
 *
 * @param maxResolutionM coarsest acceptable detail in metres; null when any resolution will do
 */
public record Requirement(MeasurementType type, Double maxResolutionM) {
}
