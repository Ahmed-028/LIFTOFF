package com.liftoff.model;

/**
 * A spacecraft bus: the body that carries, powers and talks for the instruments.
 * Figures come from a real spacecraft; the payload limits are what that spacecraft is known to have carried.
 */
public record Bus(
        String id,
        String name,
        String kidName,
        String description,
        String basedOn,
        double massKg,
        ValueBasis massBasis,
        double payloadMassKg,
        double payloadPowerW,
        ValueBasis payloadBasis,
        double downlinkMbps,
        int designLifeMonths,
        double costMusd,
        ValueBasis costBasis,
        int buildMonths,
        ValueBasis buildBasis,
        SourceRef source) {
}
