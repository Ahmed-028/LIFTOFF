package com.liftoff.model;

/**
 * One measurement an instrument can make.
 *
 * @param resolutionM size of the smallest detail it can see, in metres; null when the source does not state it
 */
public record Capability(MeasurementType type, Double resolutionM) {
}
