package com.liftoff.model;

import java.util.List;
import java.util.Objects;

/**
 * A playable mission. Successor of {@link Objective}: same fields plus difficulty, destination,
 * ladder, trip delta-v, crew supplies and how the flight is stepped through time.
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
        StepUnit stepUnit,              // what one simulation step stands for
        int steps,                      // how many steps the flight lasts; 0 means "the design's science months"
        int crew,                       // people on board; 0 for a robotic mission
        double surfaceDeltaVRequiredMs, // the part of deltaVRequiredMs flown by the surface stage; 0 if none
        SourceRef source) {

    /** A robotic mission stepped in months for as long as the design's science duration, with no surface leg. */
    public Mission(String id, String title, String question, String description, String basedOn,
                   Difficulty difficulty, Destination destination, boolean ladder,
                   double targetLatitudeDeg, boolean prefersSunSynchronous,
                   List<Requirement> required, List<MeasurementType> bonus,
                   double budgetCapMusd, int deadlineMonths, int minScienceMonths,
                   double deltaVRequiredMs, double suppliesKgPerCrewDay, SourceRef source) {
        this(id, title, question, description, basedOn, difficulty, destination, ladder, targetLatitudeDeg,
                prefersSunSynchronous, required, bonus, budgetCapMusd, deadlineMonths, minScienceMonths,
                deltaVRequiredMs, suppliesKgPerCrewDay, StepUnit.MONTH, 0, 0, 0, source);
    }

    public Mission {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(difficulty, "difficulty");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(stepUnit, "stepUnit");
        required = List.copyOf(required);
        bonus = List.copyOf(bonus);
        requireAmount(budgetCapMusd, "budgetCapMusd", id);
        requireAmount(deltaVRequiredMs, "deltaVRequiredMs", id);
        requireAmount(suppliesKgPerCrewDay, "suppliesKgPerCrewDay", id);
        requireAmount(surfaceDeltaVRequiredMs, "surfaceDeltaVRequiredMs", id);
        requireAmount(steps, "steps", id);
        requireAmount(crew, "crew", id);
        if (surfaceDeltaVRequiredMs > deltaVRequiredMs) {
            throw new IllegalArgumentException("Mission " + id + ": the surface delta-v is part of the trip's, "
                    + "so it cannot be larger");
        }
    }

    /** Zero or more, and a real number. */
    private static void requireAmount(double value, String field, String id) {
        if (!(value >= 0) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("Mission " + id + ": " + field + " must be zero or more, was " + value);
        }
    }
}
