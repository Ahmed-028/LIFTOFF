package com.liftoff.data;

import com.liftoff.model.Destination;
import com.liftoff.model.Difficulty;
import com.liftoff.model.Mission;
import com.liftoff.model.Objective;
import com.liftoff.model.SourceRef;
import com.liftoff.model.StepUnit;
import com.liftoff.model.ValueBasis;

import java.util.Objects;

/** Bundled briefing and flight parameters for a mission. */
public record MissionEntry(Objective briefing, Difficulty difficulty, Destination destination,
                           boolean ladder, StepUnit stepUnit, int steps, int crew,
                           double suppliesKgPerCrewDay, SourceRef suppliesSource,
                           Double deltaVRequiredMs, ValueBasis deltaVBasis, SourceRef deltaVSource,
                           double surfaceDeltaVRequiredMs, double downlinkMbps, double contactMinutesPerDay,
                           SourceRef downlinkSource) {
    public MissionEntry {
        Objects.requireNonNull(briefing, "mission needs a briefing");
    }

    public String id() {
        return briefing.id();
    }

    public SourceRef source() {
        return briefing.source();
    }

    /** The mission the engine works with: the briefing and the flight parameters in one record. */
    public Mission toMission() {
        return new Mission(briefing.id(), briefing.title(), briefing.question(), briefing.description(),
                briefing.basedOn(), difficulty, destination, ladder, briefing.targetLatitudeDeg(),
                briefing.prefersSunSynchronous(), briefing.required(), briefing.bonus(),
                briefing.budgetCapMusd(), briefing.deadlineMonths(), briefing.minScienceMonths(),
                deltaVRequiredMs == null ? 0 : deltaVRequiredMs, suppliesKgPerCrewDay,
                stepUnit, steps, crew, surfaceDeltaVRequiredMs, briefing.source());
    }
}
