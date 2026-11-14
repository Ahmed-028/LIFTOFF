package com.liftoff.engine.sim;

import com.liftoff.session.stub.Mission.StepUnit;

import java.util.List;


public record SimulationResult(StepUnit stepUnit, List<SimStep> timeline, List<SimEvent> events,
                               double totalProducedGbit, double totalSentGbit, double totalLostGbit,
                               double storedAtEndGbit, int launchSlipMonths,
                               SimLimit firstLimit, int firstLimitIndex, boolean success) {

    public SimulationResult {
        timeline = List.copyOf(timeline);
        events = List.copyOf(events);
    }
}
