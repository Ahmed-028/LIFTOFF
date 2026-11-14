package com.liftoff.model;

import java.util.List;


public record Instrument(
        String id,
        String name,
        String kidName,
        String description,
        String flewOn,
        double massKg,
        double avgPowerW,
        ValueBasis powerBasis,
        double avgDataRateKbps,
        ValueBasis dataBasis,
        List<Capability> capabilities,
        double costMusd,
        ValueBasis costBasis,
        int buildMonths,
        ValueBasis buildBasis,
        SourceRef source) {

    private static final double SECONDS_PER_DAY = 86_400;
    private static final double KILOBITS_PER_GIGABIT = 1_000_000;

    public Instrument {
        capabilities = List.copyOf(capabilities);
    }


    public double dataGbitPerDay() {
        return avgDataRateKbps * SECONDS_PER_DAY / KILOBITS_PER_GIGABIT;
    }
}
