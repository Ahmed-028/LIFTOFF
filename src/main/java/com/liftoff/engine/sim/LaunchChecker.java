package com.liftoff.engine.sim;

import com.liftoff.engine.OrbitMath;
import com.liftoff.engine.ResourceCalculator;
import com.liftoff.engine.ResourceReport;
import com.liftoff.engine.ResourceType;
import com.liftoff.engine.ResourceUsage;
import com.liftoff.engine.sim.LaunchIssue.Kind;
import com.liftoff.model.LaunchClass;
import com.liftoff.model.LaunchVehicle;
import com.liftoff.model.MissionDesign;
import com.liftoff.model.Orbit;
import com.liftoff.session.stub.BuildReport;
import com.liftoff.session.stub.Mission;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;


public final class LaunchChecker {

    private LaunchChecker() {
    }


    public static List<LaunchIssue> check(MissionDesign design, Mission mission, LaunchVehicle rocket, Orbit orbit) {
        return check(design, null, mission, rocket, orbit);
    }


    public static List<LaunchIssue> check(MissionDesign design, BuildReport build, Mission mission,
                                          LaunchVehicle rocket, Orbit orbit) {
        List<LaunchIssue> issues = new ArrayList<>();
        ResourceReport resources = ResourceCalculator.calculate(design);
        boolean earth = mission.destination() == Mission.Destination.EARTH;

        double massKg = resources.get(ResourceType.MASS).used() + (build == null ? 0 : build.totalMassKg());
        LaunchClass launchClass = switch (mission.destination()) {
            case EARTH -> orbit == null ? null : orbit.launchClass();
            case MOON -> LaunchClass.TRANS_LUNAR;
            case MARS -> LaunchClass.TRANS_MARS;
        };
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
