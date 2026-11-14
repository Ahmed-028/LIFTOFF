package com.liftoff.model;

import java.util.List;
import java.util.Objects;

// Caps and durations are game parameters; delta-v and daily supplies require cited sources.
public record Mission(
        String id,
        String title,
        String question,
        String description,
        String basedOn,
        Difficulty difficulty,
        Destination destination,
        boolean ladder,
        double targetLatitudeDeg,
        boolean prefersSunSynchronous,
        List<Requirement> required,
        List<MeasurementType> bonus,
        double budgetCapMusd,
        int deadlineMonths,
        int minScienceMonths,
        double deltaVRequiredMs,
        double suppliesKgPerCrewDay,
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


    private static void requireAmount(double value, String field, String id) {
        if (!(value >= 0) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("Mission " + id + ": " + field + " must be zero or more, was " + value);
        }
    }
}
