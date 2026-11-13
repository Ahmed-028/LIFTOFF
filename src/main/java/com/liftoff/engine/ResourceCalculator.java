package com.liftoff.engine;

import com.liftoff.model.Bus;
import com.liftoff.model.GroundStation;
import com.liftoff.model.Instrument;
import com.liftoff.model.MissionDesign;

import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalDouble;

/** Works out the six resources of a design. Pure: same design in, same report out. */
public final class ResourceCalculator {

    private static final double SECONDS_PER_MINUTE = 60;
    private static final double MEGABITS_PER_GIGABIT = 1000;

    private ResourceCalculator() {
    }

    public static ResourceReport calculate(MissionDesign design) {
        Map<ResourceType, ResourceUsage> usages = new EnumMap<>(ResourceType.class);
        usages.put(ResourceType.MASS, mass(design));
        usages.put(ResourceType.POWER, power(design));
        usages.put(ResourceType.BUDGET, budget(design));
        usages.put(ResourceType.DATA, data(design));
        usages.put(ResourceType.COMMS, comms(design));
        usages.put(ResourceType.TIME, time(design));
        return new ResourceReport(usages);
    }

    /** Bus plus instruments, against what the rocket lifts to the chosen class of orbit. */
    private static ResourceUsage mass(MissionDesign design) {
        double used = design.instruments().stream().mapToDouble(Instrument::massKg).sum();
        if (design.bus() != null) {
            used += design.bus().massKg();
        }
        OptionalDouble capacity = OptionalDouble.empty();
        if (design.launchVehicle() != null && design.orbit() != null) {
            capacity = design.launchVehicle().capacityFor(design.orbit().launchClass());
        }
        return ResourceUsage.of(ResourceType.MASS, used, capacity);
    }

    /** Average draw of the instruments, against what the bus can supply to its payload. */
    private static ResourceUsage power(MissionDesign design) {
        double used = design.instruments().stream().mapToDouble(Instrument::avgPowerW).sum();
        OptionalDouble capacity = design.bus() == null
                ? OptionalDouble.empty()
                : OptionalDouble.of(design.bus().payloadPowerW());
        return ResourceUsage.of(ResourceType.POWER, used, capacity);
    }

    /** Everything bought, plus ground stations for every science month, against the objective's cap. */
    private static ResourceUsage budget(MissionDesign design) {
        double used = design.instruments().stream().mapToDouble(Instrument::costMusd).sum();
        if (design.bus() != null) {
            used += design.bus().costMusd();
        }
        if (design.launchVehicle() != null) {
            used += design.launchVehicle().costMusd();
        }
        double stationsPerMonth = design.stations().stream().mapToDouble(GroundStation::costMusdPerMonth).sum();
        used += stationsPerMonth * design.scienceMonths();

        OptionalDouble capacity = design.objective() == null
                ? OptionalDouble.empty()
                : OptionalDouble.of(design.objective().budgetCapMusd());
        return ResourceUsage.of(ResourceType.BUDGET, used, capacity);
    }

    /** Data produced per day, against what the radio can send down in the daily contact time. */
    private static ResourceUsage data(MissionDesign design) {
        OptionalDouble capacity = OptionalDouble.empty();
        if (design.bus() != null && design.orbit() != null) {
            double megabits = design.bus().downlinkMbps() * SECONDS_PER_MINUTE * contactMinutesPerDay(design);
            capacity = OptionalDouble.of(megabits / MEGABITS_PER_GIGABIT);
        }
        return ResourceUsage.of(ResourceType.DATA, dataGbitPerDay(design), capacity);
    }

    /** Contact minutes needed to send the daily data, against the minutes the stations can see the spacecraft. */
    private static ResourceUsage comms(MissionDesign design) {
        double used = 0;
        Bus bus = design.bus();
        if (bus != null && bus.downlinkMbps() > 0) {
            double megabits = dataGbitPerDay(design) * MEGABITS_PER_GIGABIT;
            used = megabits / (bus.downlinkMbps() * SECONDS_PER_MINUTE);
        }
        OptionalDouble capacity = design.orbit() == null
                ? OptionalDouble.empty()
                : OptionalDouble.of(contactMinutesPerDay(design));
        return ResourceUsage.of(ResourceType.COMMS, used, capacity);
    }

    /** Build and test time plus the science duration, against the objective's deadline. */
    private static ResourceUsage time(MissionDesign design) {
        double used = buildMonths(design) + design.scienceMonths();
        OptionalDouble capacity = design.objective() == null
                ? OptionalDouble.empty()
                : OptionalDouble.of(design.objective().deadlineMonths());
        return ResourceUsage.of(ResourceType.TIME, used, capacity);
    }

    /** The slowest part sets the pace; integration and test are added on top. */
    private static int buildMonths(MissionDesign design) {
        if (design.bus() == null && design.instruments().isEmpty()) {
            return 0;
        }
        int slowestPart = design.instruments().stream().mapToInt(Instrument::buildMonths).max().orElse(0);
        if (design.bus() != null) {
            slowestPart = Math.max(slowestPart, design.bus().buildMonths());
        }
        return slowestPart
                + GameRules.INTEGRATION_BASE_MONTHS
                + GameRules.INTEGRATION_MONTHS_PER_INSTRUMENT * design.instruments().size();
    }

    private static double dataGbitPerDay(MissionDesign design) {
        return design.instruments().stream().mapToDouble(Instrument::dataGbitPerDay).sum();
    }

    /** Contact from several stations is summed: a small overcount when two see the spacecraft at once. */
    private static double contactMinutesPerDay(MissionDesign design) {
        return design.stations().stream()
                .mapToDouble(station -> OrbitMath.contactMinutesPerDay(design.orbit(), station.latitudeDeg()))
                .sum();
    }
}
