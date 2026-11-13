package com.liftoff.engine;

import com.liftoff.model.MeasurementType;

import java.util.Objects;

/**
 * One way the design fails the mission's science goal, with the two numbers the sentence needs.
 * measurement is the measurement concerned; null for ORBIT_MISSES_TARGET.
 */
public record DesignIssue(Kind kind, MeasurementType measurement, double needed, double available) {

    public enum Kind {
        /**
         * No instrument makes a required measurement. needed is the coarsest acceptable detail
         * in metres (0 when any will do); available is 0.
         */
        MEASUREMENT_MISSING,
        /**
         * Instruments make the measurement, but none sharply enough. needed is the coarsest
         * acceptable detail in metres; available is the finest detail the design has, or NaN
         * when no instrument's source states a resolution.
         */
        RESOLUTION_TOO_COARSE,
        /**
         * The orbit never flies over the target. needed is the target latitude in degrees
         * (north or south, as a positive number); available is the highest latitude the orbit reaches.
         */
        ORBIT_MISSES_TARGET
    }

    public DesignIssue {
        Objects.requireNonNull(kind, "kind");
    }
}
