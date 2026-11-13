package com.liftoff.model;

/** A ground station that can receive data from the spacecraft. */
public record GroundStation(
        String id,
        String name,
        String kidName,
        double latitudeDeg,
        double longitudeDeg,
        String bands,
        double costMusdPerMonth,
        ValueBasis costBasis,
        SourceRef source) {
}
