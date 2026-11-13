package com.liftoff.engine.score;

import com.liftoff.engine.ResourceLevel;
import com.liftoff.engine.ResourceReport;
import com.liftoff.engine.ResourceType;
import com.liftoff.engine.ResourceUsage;
import com.liftoff.engine.sim.SimRules;
import com.liftoff.engine.sim.SimulationResult;
import com.liftoff.model.Capability;
import com.liftoff.model.Instrument;
import com.liftoff.model.MissionDesign;
import com.liftoff.model.Requirement;
import com.liftoff.session.stub.BuildReport;
import com.liftoff.session.stub.Mission;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/** Turns a flown mission into a score out of 100 and one to three stars. Pure. */
public final class ScoreCalculator {

    private ScoreCalculator() {
    }

    /**
     * @param build the ship's report, or null when the design has no modular ship
     * @param sim   the flight, or null when the mission never left the pad
     */
    public static Score score(MissionDesign design, ResourceReport resources, BuildReport build,
                              SimulationResult sim, Mission mission) {
        return score(design, resources, build, sim, mission, false);
    }

    /** Scores a report whose budget already includes the modular ship. */
    public static Score scoreCombined(MissionDesign design, ResourceReport resources, BuildReport build,
                                      SimulationResult sim, Mission mission) {
        return score(design, resources, build, sim, mission, true);
    }

    private static Score score(MissionDesign design, ResourceReport resources, BuildReport build,
                               SimulationResult sim, Mission mission, boolean includesShipCost) {
        double science = ScoreRules.SCIENCE_POINTS * scienceFraction(design, sim, mission);
        double margins = ScoreRules.MARGIN_POINTS * marginShare(resources, build, mission);

        ResourceUsage budget = resources.get(ResourceType.BUDGET);
        ResourceUsage time = resources.get(ResourceType.TIME);
        double costMusd = budget.used() + (includesShipCost || build == null ? 0 : build.costMusd());
        double months = time.used() + (sim == null ? 0 : sim.launchSlipMonths());
        double budgetTime = ScoreRules.BUDGET_POINTS * withinCap(costMusd, budget.capacity())
                + ScoreRules.TIME_POINTS * withinCap(months, time.capacity());

        double total = science + margins + budgetTime;
        return new Score(total, science, margins, budgetTime, stars(total));
    }

    public static int stars(double total) {
        if (total >= ScoreRules.THREE_STARS_FROM) {
            return 3;
        }
        return total >= ScoreRules.TWO_STARS_FROM ? 2 : 1;
    }

    /**
     * Share (0 to 1) of the required data that came home. Zero when the mission never flew or when
     * the design cannot make a measurement the goal must have.
     */
    public static double scienceFraction(MissionDesign design, SimulationResult sim, Mission mission) {
        if (sim == null || !unmetRequirements(design).isEmpty()) {
            return 0;
        }
        double required = requiredDataGbit(design, mission);
        if (required <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(1, sim.totalSentGbit() / required));
    }

    /**
     * What the mission asks to be returned: everything the chosen instruments produce at their
     * average rate over the planned flight, and at least over the goal's minimum science duration.
     */
    public static double requiredDataGbit(MissionDesign design, Mission mission) {
        double perDay = design.instruments().stream().mapToDouble(Instrument::dataGbitPerDay).sum();
        boolean inDays = mission.stepUnit() == Mission.StepUnit.DAY;
        int steps = mission.steps() > 0 ? mission.steps() : design.scienceMonths();
        if (!inDays && design.objective() != null) {
            steps = Math.max(steps, design.objective().minScienceMonths());
        }
        return perDay * steps * (inDays ? 1 : SimRules.DAYS_PER_MONTH);
    }

    /** The goal's must-have measurements that no chosen instrument makes finely enough. */
    public static List<Requirement> unmetRequirements(MissionDesign design) {
        List<Requirement> unmet = new ArrayList<>();
        if (design.objective() == null) {
            return unmet;
        }
        for (Requirement requirement : design.objective().required()) {
            boolean met = design.instruments().stream()
                    .flatMap(instrument -> instrument.capabilities().stream())
                    .anyMatch(capability -> meets(capability, requirement));
            if (!met) {
                unmet.add(requirement);
            }
        }
        return unmet;
    }

    private static boolean meets(Capability capability, Requirement requirement) {
        if (capability.type() != requirement.type()) {
            return false;
        }
        if (requirement.maxResolutionM() == null) {
            return true;
        }
        return capability.resolutionM() != null && capability.resolutionM() <= requirement.maxResolutionM();
    }

    /** Average margin credit over the six resources, plus fuel beyond Earth orbit. */
    private static double marginShare(ResourceReport resources, BuildReport build, Mission mission) {
        double credit = 0;
        int counted = 0;
        for (ResourceType type : ResourceType.values()) {
            credit += credit(resources.get(type).level());
            counted++;
        }
        if (mission.destination() != Mission.Destination.EARTH) {
            credit += credit(build == null ? ResourceLevel.UNKNOWN : build.fuelLevel());
            counted++;
        }
        return credit / counted;
    }

    private static double credit(ResourceLevel level) {
        return switch (level) {
            case OK -> 1;
            case TIGHT -> ScoreRules.TIGHT_CREDIT;
            case OVER, UNKNOWN -> 0;
        };
    }

    /** 1 when inside the cap, cap / used when over it, 0 when the cap is not known. */
    private static double withinCap(double used, OptionalDouble cap) {
        if (cap.isEmpty()) {
            return 0;
        }
        return used <= cap.getAsDouble() ? 1 : Math.max(0, cap.getAsDouble() / used);
    }
}
