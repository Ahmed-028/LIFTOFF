package com.liftoff.model;


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
