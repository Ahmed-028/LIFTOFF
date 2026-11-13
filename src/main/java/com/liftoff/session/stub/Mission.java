package com.liftoff.session.stub;

/**
 * TODO(Member 2): stub of model/Mission (the renamed Objective). Only what the flow and the
 * simulator need; the real one also has difficulty and the caps.
 *
 * @param steps                how many time steps the flight lasts; 0 means "the design's science months"
 * @param crew                 people on board; 0 for a robotic mission
 * @param suppliesKgPerCrewDay sourced daily supply mass for each crew member
 */
public record Mission(String id, Destination destination, StepUnit stepUnit, int steps,
                      double deltaVRequiredMs, int crew, double suppliesKgPerCrewDay,
                      double surfaceDeltaVRequiredMs) {

    public Mission(String id, Destination destination, StepUnit stepUnit, int steps,
                   double deltaVRequiredMs, int crew, double suppliesKgPerCrewDay) {
        this(id, destination, stepUnit, steps, deltaVRequiredMs, crew, suppliesKgPerCrewDay, 0);
    }

    public enum Destination { EARTH, MOON, MARS }

    /** What one simulation step stands for. */
    public enum StepUnit { MONTH, DAY }

    /** A robotic Earth mission stepped in months for as long as the design's science duration. */
    public Mission(String id, double deltaVRequiredMs) {
        this(id, Destination.EARTH, StepUnit.MONTH, 0, deltaVRequiredMs, 0, 0);
    }
}
