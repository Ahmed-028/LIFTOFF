package com.liftoff.model;

import java.util.List;

/** A science instrument, with the figures of the real instrument it is based on. */
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

    /** Data produced in one day at the average rate, in gigabits. */
    public double dataGbitPerDay() {
        return avgDataRateKbps * SECONDS_PER_DAY / KILOBITS_PER_GIGABIT;
    }
}
