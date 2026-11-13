package com.liftoff.engine.sim;

import com.liftoff.session.stub.Mission.StepUnit;

import java.util.List;

/**
 * A whole flight: the timeline the Fly screen plays back, and the totals the score is made from.
 *
 * @param storedAtEndGbit  data still on board when the flight ended (produced, never sent, not lost)
 * @param launchSlipMonths months added before the flight by launch-slip events
 * @param firstLimit       the first limit that broke, or NONE
 * @param firstLimitIndex  the step at which it broke, or -1
 * @param success          the flight ran to its end and returned some data
 */
public record SimulationResult(StepUnit stepUnit, List<SimStep> timeline, List<SimEvent> events,
                               double totalProducedGbit, double totalSentGbit, double totalLostGbit,
                               double storedAtEndGbit, int launchSlipMonths,
                               SimLimit firstLimit, int firstLimitIndex, boolean success) {

    public SimulationResult {
        timeline = List.copyOf(timeline);
        events = List.copyOf(events);
    }
}
