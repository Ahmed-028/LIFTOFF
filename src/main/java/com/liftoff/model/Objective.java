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

    /**
     * How useful an instrument is to this goal, for listing the best first: 0 if it makes a
     * required measurement (sharp enough or not), 1 if it only makes a bonus one, 2 otherwise.
     */
    public int relevanceOf(Instrument instrument) {
        if (instrument.capabilities().stream().anyMatch(capability -> required.stream()
                .anyMatch(requirement -> requirement.type() == capability.type()))) {
            return 0;
        }
        return instrument.capabilities().stream().anyMatch(capability -> bonus.contains(capability.type())) ? 1 : 2;
    }
}
