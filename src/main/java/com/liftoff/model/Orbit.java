package com.liftoff.model;


public record Orbit(
        String id,
        String name,
        String kidName,
        String description,
        String basedOn,
        double altitudeKm,
        double inclinationDeg,
        boolean sunSynchronous,
        LaunchClass launchClass,
        SourceRef source) {
}
