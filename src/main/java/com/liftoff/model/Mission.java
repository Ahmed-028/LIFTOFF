package com.liftoff.model;

import java.util.List;
import java.util.Objects;

/**
 * A playable mission. Successor of {@link Objective}: same fields plus difficulty, destination,
 * ladder, trip delta-v and crew supplies. Objective is removed once the loader reads missions.json.
 * Caps and durations are scenario settings; deltaVRequiredMs and suppliesKgPerCrewDay must be sourced.
 */
public record Mission(
        String id,
        String title,
        String question,
        String description,
        String basedOn,
        Difficulty difficulty,
        Destination destination,
        boolean ladder,                 // true for the three main missions, false for the extra Earth ones
        double targetLatitudeDeg,
        boolean prefersSunSynchronous,
        List<Requirement> required,
        List<MeasurementType> bonus,
        double budgetCapMusd,
        int deadlineMonths,
        int minScienceMonths,
        double deltaVRequiredMs,        // total speed change the trip needs, m/s
        double suppliesKgPerCrewDay,    // food, water, oxygen per person per day; 0 when uncrewed
        SourceRef source) {

    public Mission {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(difficulty, "difficulty");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(source, "source");
        required = List.copyOf(required);
        bonus = List.copyOf(bonus);
        requireAmount(budgetCapMusd, "budgetCapMusd", id);
        requireAmount(deltaVRequiredMs, "deltaVRequiredMs", id);
        requireAmount(suppliesKgPerCrewDay, "suppliesKgPerCrewDay", id);
    }

    /** Zero or more, and a real number. */
    private static void requireAmount(double value, String field, String id) {
        if (!(value >= 0) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("Mission " + id + ": " + field + " must be zero or more, was " + value);
        }
    }
}
