package com.liftoff.data;

import com.liftoff.engine.sim.Difficulty;
import com.liftoff.model.Objective;
import com.liftoff.model.SourceRef;
import com.liftoff.model.ValueBasis;
import com.liftoff.session.stub.Mission;

import java.util.Objects;


public record MissionEntry(Objective briefing, Difficulty difficulty, Mission.Destination destination,
                           boolean ladder, Mission.StepUnit stepUnit, int steps, int crew,
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
}
