package com.liftoff.session.stub;


public record Mission(String id, Destination destination, StepUnit stepUnit, int steps,
                      double deltaVRequiredMs, int crew, double suppliesKgPerCrewDay,
                      double surfaceDeltaVRequiredMs) {

    public Mission(String id, Destination destination, StepUnit stepUnit, int steps,
                   double deltaVRequiredMs, int crew, double suppliesKgPerCrewDay) {
        this(id, destination, stepUnit, steps, deltaVRequiredMs, crew, suppliesKgPerCrewDay, 0);
    }

    public enum Destination { EARTH, MOON, MARS }


    public enum StepUnit { MONTH, DAY }


    public Mission(String id, double deltaVRequiredMs) {
        this(id, Destination.EARTH, StepUnit.MONTH, 0, deltaVRequiredMs, 0, 0);
    }
}
