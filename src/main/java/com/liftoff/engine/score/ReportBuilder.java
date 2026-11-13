package com.liftoff.engine.score;

import com.liftoff.engine.ResourceReport;
import com.liftoff.engine.ResourceType;
import com.liftoff.engine.ResourceUsage;
import com.liftoff.engine.sim.LaunchIssue;
import com.liftoff.engine.sim.SimLimit;
import com.liftoff.engine.sim.SimStep;
import com.liftoff.engine.sim.SimulationResult;
import com.liftoff.model.Capability;
import com.liftoff.model.Mission;
import com.liftoff.model.MissionDesign;
import com.liftoff.model.Requirement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Explains a mission: what worked, what broke first, and one thing to try. Pure.
 *
 * <p>"Broke first" is the earliest problem in the life of the mission, in this fixed order: a
 * measurement the design cannot make (wrong from the drawing board), then launch issues in the
 * order of {@link LaunchIssue.Kind}, then the first limit the flight ran into.
 */
public final class ReportBuilder {

    public static final String WORKED_MEASUREMENTS = "report.worked.measurements";
    public static final String WORKED_LAUNCH = "report.worked.launch";
    /** Params: gigabits sent, gigabits produced. */
    public static final String WORKED_DATA_RETURNED = "report.worked.dataReturned";
    public static final String WORKED_NO_DATA_LOST = "report.worked.noDataLost";
    public static final String WORKED_POWER_ENOUGH = "report.worked.powerEnough";

    public static final String BROKE_NOTHING = "report.broke.nothing";
    /** Params: measurement type. */
    public static final String BROKE_REQUIREMENT_MISSING = "report.broke.requirementMissing";
    /** Params: measurement type, metres needed, metres of the best instrument on board. */
    public static final String BROKE_REQUIREMENT_TOO_COARSE = "report.broke.requirementTooCoarse";
    /** Launch issues. Params: needed, available (kg, m/s, degrees, $M or months). */
    public static final String BROKE_MASS_OVER_CAPACITY = "report.broke.massOverCapacity";
    public static final String BROKE_ROCKET_NOT_OFFERED = "report.broke.rocketNotOffered";
    public static final String BROKE_FUEL_SHORT = "report.broke.fuelShort";
    public static final String BROKE_ORBIT_CANNOT_REACH_TARGET = "report.broke.orbitCannotReachTarget";
    public static final String BROKE_OVER_BUDGET = "report.broke.overBudget";
    public static final String BROKE_OVER_DEADLINE = "report.broke.overDeadline";
    /** Flight limits. Params: step number from 1, step unit (MONTH or DAY), then the amount. */
    public static final String BROKE_POWER = "report.broke.power";
    public static final String BROKE_DOWNLINK = "report.broke.downlink";
    public static final String BROKE_STORAGE = "report.broke.storage";
    /** Params: step number from 1, step unit. */
    public static final String BROKE_SUPPLIES = "report.broke.supplies";
    public static final String BROKE_NO_DATA_RETURNED = "report.broke.noDataReturned";

    /** Params: resource type, used, limit, unit. */
    public static final String TRY_THINNEST_MARGIN = "report.try.thinnestMargin";
    public static final String TRY_HARDER_MISSION = "report.try.harderMission";
    /** Params: measurement type. */
    public static final String TRY_ADD_INSTRUMENT_FOR = "report.try.addInstrumentFor";
    /** Params: measurement type, metres needed. */
    public static final String TRY_SHARPER_INSTRUMENT = "report.try.sharperInstrument";
    /** Params: kg to lift. */
    public static final String TRY_BIGGER_ROCKET_OR_LIGHTER_SHIP = "report.try.biggerRocketOrLighterShip";
    public static final String TRY_OTHER_ROCKET = "report.try.otherRocket";
    public static final String TRY_MORE_FUEL = "report.try.moreFuel";
    /** Params: target latitude in degrees. */
    public static final String TRY_STEEPER_ORBIT = "report.try.steeperOrbit";
    /** Params: $M over the cap. */
    public static final String TRY_CHEAPER_PARTS = "report.try.cheaperParts";
    /** Params: months over the deadline. */
    public static final String TRY_SHORTER_SCIENCE_OR_FASTER_PARTS = "report.try.shorterScienceOrFasterParts";
    /** Params: watts short. */
    public static final String TRY_MORE_POWER_OR_FEWER_INSTRUMENTS = "report.try.morePowerOrFewerInstruments";
    public static final String TRY_ADD_STATION = "report.try.addStation";
    public static final String TRY_ADD_STATION_OR_FASTER_RADIO = "report.try.addStationOrFasterRadio";
    public static final String TRY_MORE_SUPPLIES_OR_SHORTER_STAY = "report.try.moreSuppliesOrShorterStay";
    public static final String TRY_ADD_INSTRUMENT = "report.try.addInstrument";

    private ReportBuilder() {
    }

    /** Every message key a report can carry, for the interface texts. */
    public static List<String> messageKeys() {
        return List.of(WORKED_MEASUREMENTS, WORKED_LAUNCH, WORKED_DATA_RETURNED, WORKED_NO_DATA_LOST,
                WORKED_POWER_ENOUGH, BROKE_NOTHING, BROKE_REQUIREMENT_MISSING, BROKE_REQUIREMENT_TOO_COARSE,
                BROKE_MASS_OVER_CAPACITY, BROKE_ROCKET_NOT_OFFERED, BROKE_FUEL_SHORT,
                BROKE_ORBIT_CANNOT_REACH_TARGET, BROKE_OVER_BUDGET, BROKE_OVER_DEADLINE, BROKE_POWER,
                BROKE_DOWNLINK, BROKE_STORAGE, BROKE_SUPPLIES, BROKE_NO_DATA_RETURNED, TRY_THINNEST_MARGIN,
                TRY_HARDER_MISSION, TRY_ADD_INSTRUMENT_FOR, TRY_SHARPER_INSTRUMENT,
                TRY_BIGGER_ROCKET_OR_LIGHTER_SHIP, TRY_OTHER_ROCKET, TRY_MORE_FUEL, TRY_STEEPER_ORBIT,
                TRY_CHEAPER_PARTS, TRY_SHORTER_SCIENCE_OR_FASTER_PARTS, TRY_MORE_POWER_OR_FEWER_INSTRUMENTS,
                TRY_ADD_STATION, TRY_ADD_STATION_OR_FASTER_RADIO, TRY_MORE_SUPPLIES_OR_SHORTER_STAY,
                TRY_ADD_INSTRUMENT);
    }

    /**
     * @param launchIssues what the launch check found; empty when it passed
     * @param sim          the flight, or null when the mission never left the pad
     */
    public static Report build(MissionDesign design, ResourceReport resources, List<LaunchIssue> launchIssues,
                               SimulationResult sim, Mission mission) {
        List<Requirement> unmet = ScoreCalculator.unmetRequirements(design);
        List<WhyCard> worked = worked(design, unmet, launchIssues, sim);

        if (!unmet.isEmpty()) {
            return requirementReport(design, unmet.get(0), worked);
        }
        if (!launchIssues.isEmpty()) {
            LaunchIssue first = launchIssues.stream().min(Comparator.comparing(LaunchIssue::kind)).orElseThrow();
            return launchReport(first, worked);
        }
        if (sim != null && sim.firstLimit() != SimLimit.NONE) {
            return flightReport(design, sim, worked);
        }
        if (sim != null && !sim.success()) {
            return new Report(worked, WhyCard.of(BROKE_NO_DATA_RETURNED),
                    WhyCard.of(design.instruments().isEmpty() ? TRY_ADD_INSTRUMENT : TRY_ADD_STATION));
        }
        return new Report(worked, WhyCard.of(BROKE_NOTHING), thinnestMargin(resources));
    }

    private static List<WhyCard> worked(MissionDesign design, List<Requirement> unmet,
                                        List<LaunchIssue> launchIssues, SimulationResult sim) {
        List<WhyCard> worked = new ArrayList<>();
        if (design.objective() != null && !design.objective().required().isEmpty() && unmet.isEmpty()) {
            worked.add(WhyCard.of(WORKED_MEASUREMENTS));
        }
        if (launchIssues.isEmpty()) {
            worked.add(WhyCard.of(WORKED_LAUNCH));
        }
        if (sim != null) {
            if (sim.totalSentGbit() > 0) {
                worked.add(WhyCard.of(WORKED_DATA_RETURNED, sim.totalSentGbit(), sim.totalProducedGbit()));
            }
            if (sim.totalProducedGbit() > 0 && sim.totalLostGbit() == 0) {
                worked.add(WhyCard.of(WORKED_NO_DATA_LOST));
            }
            if (!sim.timeline().isEmpty() && sim.timeline().stream().noneMatch(step -> step.powerShortfallW() > 0)) {
                worked.add(WhyCard.of(WORKED_POWER_ENOUGH));
            }
        }
        return worked;
    }

    private static Report requirementReport(MissionDesign design, Requirement requirement, List<WhyCard> worked) {
        String type = requirement.type().name();
        OptionalDouble finestOnBoard = design.instruments().stream()
                .flatMap(instrument -> instrument.capabilities().stream())
                .filter(capability -> capability.type() == requirement.type() && capability.resolutionM() != null)
                .mapToDouble(Capability::resolutionM)
                .min();
        if (requirement.maxResolutionM() != null && finestOnBoard.isPresent()) {
            return new Report(worked,
                    WhyCard.of(BROKE_REQUIREMENT_TOO_COARSE, type, requirement.maxResolutionM(),
                            finestOnBoard.getAsDouble()),
                    WhyCard.of(TRY_SHARPER_INSTRUMENT, type, requirement.maxResolutionM()));
        }
        return new Report(worked, WhyCard.of(BROKE_REQUIREMENT_MISSING, type),
                WhyCard.of(TRY_ADD_INSTRUMENT_FOR, type));
    }

    private static Report launchReport(LaunchIssue issue, List<WhyCard> worked) {
        double needed = issue.needed();
        double available = issue.available();
        double over = needed - available;
        return switch (issue.kind()) {
            case MASS_OVER_CAPACITY -> new Report(worked, WhyCard.of(BROKE_MASS_OVER_CAPACITY, needed, available),
                    WhyCard.of(TRY_BIGGER_ROCKET_OR_LIGHTER_SHIP, needed));
            case ROCKET_NOT_OFFERED -> new Report(worked, WhyCard.of(BROKE_ROCKET_NOT_OFFERED, needed, available),
                    WhyCard.of(TRY_OTHER_ROCKET));
            case FUEL_SHORT -> new Report(worked, WhyCard.of(BROKE_FUEL_SHORT, needed, available),
                    WhyCard.of(TRY_MORE_FUEL));
            case ORBIT_CANNOT_REACH_TARGET -> new Report(worked,
                    WhyCard.of(BROKE_ORBIT_CANNOT_REACH_TARGET, needed, available),
                    WhyCard.of(TRY_STEEPER_ORBIT, needed));
            case OVER_BUDGET -> new Report(worked, WhyCard.of(BROKE_OVER_BUDGET, needed, available),
                    WhyCard.of(TRY_CHEAPER_PARTS, over));
            case OVER_DEADLINE -> new Report(worked, WhyCard.of(BROKE_OVER_DEADLINE, needed, available),
                    WhyCard.of(TRY_SHORTER_SCIENCE_OR_FASTER_PARTS, over));
        };
    }

    private static Report flightReport(MissionDesign design, SimulationResult sim, List<WhyCard> worked) {
        SimStep step = sim.timeline().get(sim.firstLimitIndex());
        int number = sim.firstLimitIndex() + 1;
        String unit = sim.stepUnit().name();
        return switch (sim.firstLimit()) {
            case POWER -> new Report(worked, WhyCard.of(BROKE_POWER, number, unit, step.powerShortfallW()),
                    WhyCard.of(TRY_MORE_POWER_OR_FEWER_INSTRUMENTS, step.powerShortfallW()));
            case DOWNLINK -> new Report(worked, WhyCard.of(BROKE_DOWNLINK, number, unit, step.storageUsedGbit()),
                    WhyCard.of(design.stations().isEmpty() ? TRY_ADD_STATION : TRY_ADD_STATION_OR_FASTER_RADIO));
            case STORAGE -> new Report(worked, WhyCard.of(BROKE_STORAGE, number, unit, step.dataLostGbit()),
                    WhyCard.of(TRY_ADD_STATION));
            case SUPPLIES -> new Report(worked, WhyCard.of(BROKE_SUPPLIES, number, unit),
                    WhyCard.of(TRY_MORE_SUPPLIES_OR_SHORTER_STAY));
            case NONE -> throw new IllegalStateException("No limit broke");
        };
    }

    /** The resource closest to its limit; ties go to the first in the order of ResourceType. */
    private static WhyCard thinnestMargin(ResourceReport resources) {
        ResourceUsage thinnest = null;
        double fullest = -1;
        for (ResourceType type : ResourceType.values()) {
            ResourceUsage usage = resources.get(type);
            if (usage.capacity().isPresent() && usage.capacity().getAsDouble() > 0) {
                double share = usage.used() / usage.capacity().getAsDouble();
                if (share > fullest) {
                    fullest = share;
                    thinnest = usage;
                }
            }
        }
        if (thinnest == null) {
            return WhyCard.of(TRY_HARDER_MISSION);
        }
        return WhyCard.of(TRY_THINNEST_MARGIN, thinnest.type().name(), thinnest.used(),
                thinnest.capacity().getAsDouble(), thinnest.type().unit());
    }
}
