package com.liftoff.model;

import java.util.List;

/** A mission goal the player can choose. Caps and durations are scenario settings, not NASA data. */
public record Objective(
        String id,
        String title,
        String question,
        String description,
        String basedOn,
        double targetLatitudeDeg,
        boolean prefersSunSynchronous,
        List<Requirement> required,
        List<MeasurementType> bonus,
        double budgetCapMusd,
        int deadlineMonths,
        int minScienceMonths,
        SourceRef source) {

    public Objective {
        required = List.copyOf(required);
        bonus = List.copyOf(bonus);
    }
}
