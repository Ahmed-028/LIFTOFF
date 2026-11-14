package com.liftoff.model;


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
        SourceRef source,
        Double storageGbit) {

    public Bus(String id, String name, String kidName, String description, String basedOn,
               double massKg, ValueBasis massBasis, double payloadMassKg, double payloadPowerW,
               ValueBasis payloadBasis, double downlinkMbps, int designLifeMonths, double costMusd,
               ValueBasis costBasis, int buildMonths, ValueBasis buildBasis, SourceRef source) {
        this(id, name, kidName, description, basedOn, massKg, massBasis, payloadMassKg, payloadPowerW,
                payloadBasis, downlinkMbps, designLifeMonths, costMusd, costBasis, buildMonths, buildBasis, source, null);
    }
}
