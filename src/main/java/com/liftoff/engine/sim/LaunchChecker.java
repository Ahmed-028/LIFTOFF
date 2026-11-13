package com.liftoff.engine.sim;

import com.liftoff.engine.BuildReport;
import com.liftoff.engine.OrbitMath;
import com.liftoff.engine.ResourceCalculator;
import com.liftoff.engine.ResourceReport;
import com.liftoff.engine.ResourceType;
import com.liftoff.engine.ResourceUsage;
import com.liftoff.engine.sim.LaunchIssue.Kind;
import com.liftoff.model.Destination;
import com.liftoff.model.LaunchClass;
import com.liftoff.model.LaunchVehicle;
import com.liftoff.model.Mission;
import com.liftoff.model.MissionDesign;
import com.liftoff.model.Orbit;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/** The checks run on the launch pad. Returns every reason the mission cannot fly, not just the first. */
public final class LaunchChecker {

    private LaunchChecker() {
    }

    /** Checks a design that has no modular ship (no propellant, no fuel check). */
    public static List<LaunchIssue> check(MissionDesign design, Mission mission, LaunchVehicle rocket, Orbit orbit) {
        return check(design, null, mission, rocket, orbit);
    }

    /**
     * @param build the ship's report, or null when the design has no modular ship; its total mass
     *              (propellant included) and cost are added to the design's
     */
    public static List<LaunchIssue> check(MissionDesign design, BuildReport build, Mission mission,
                                          LaunchVehicle rocket, Orbit orbit) {
        List<LaunchIssue> issues = new ArrayList<>();
        ResourceReport resources = ResourceCalculator.calculate(design);
        boolean earth = mission.destination() == Destination.EARTH_ORBIT;

        double massKg = resources.get(ResourceType.MASS).used() + (build == null ? 0 : build.totalMassKg());
        LaunchClass launchClass = mission.destination().launchClass(orbit);
        OptionalDouble capacityKg = rocket == null || launchClass == null
                ? OptionalDouble.empty() : rocket.capacityFor(launchClass);
        if (capacityKg.isEmpty()) {
            issues.add(LaunchIssue.of(Kind.ROCKET_NOT_OFFERED, massKg, 0));
        } else if (massKg > capacityKg.getAsDouble()) {
            issues.add(LaunchIssue.of(Kind.MASS_OVER_CAPACITY, massKg, capacityKg.getAsDouble()));
        }

        if (build != null && build.deltaVAvailableMs() < build.deltaVRequiredMs()) {
            issues.add(LaunchIssue.of(Kind.FUEL_SHORT, build.deltaVRequiredMs(), build.deltaVAvailableMs()));
        }

        if (earth && design.objective() != null) {
            double targetDeg = design.objective().targetLatitudeDeg();
            double reachDeg = orbit == null ? 0 : OrbitMath.maxLatitudeDeg(orbit.inclinationDeg());
            if (reachDeg < targetDeg) {
                issues.add(LaunchIssue.of(Kind.ORBIT_CANNOT_REACH_TARGET, targetDeg, reachDeg));
            }
        }

        ResourceUsage budget = resources.get(ResourceType.BUDGET);
        double costMusd = budget.used() + (build == null ? 0 : build.costMusd());
        if (budget.capacity().isPresent() && costMusd > budget.capacity().getAsDouble()) {
            issues.add(LaunchIssue.of(Kind.OVER_BUDGET, costMusd, budget.capacity().getAsDouble()));
        }

        ResourceUsage time = resources.get(ResourceType.TIME);
        if (time.capacity().isPresent() && time.used() > time.capacity().getAsDouble()) {
            issues.add(LaunchIssue.of(Kind.OVER_DEADLINE, time.used(), time.capacity().getAsDouble()));
        }
        return List.copyOf(issues);
    }
}
