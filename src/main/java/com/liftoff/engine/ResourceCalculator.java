package com.liftoff.engine;

import com.liftoff.model.Bus;
import com.liftoff.model.GroundStation;
import com.liftoff.model.Instrument;
import com.liftoff.model.MissionDesign;
import com.liftoff.model.ModuleSlot;
import com.liftoff.model.ShipBuild;
import com.liftoff.model.ShipPart;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.ToDoubleFunction;


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


    public static Map<ResourceType, ResourceUsage> communication(MissionDesign design, double downlinkMbps,
                                                                  OptionalDouble fixedContactMinutes,
                                                                  double additionalDataGbitPerDay) {
        OptionalDouble contact = fixedContactMinutes.isPresent()
                ? OptionalDouble.of(design.stations().isEmpty() ? 0 : fixedContactMinutes.getAsDouble())
                : design.orbit() == null ? OptionalDouble.empty() : OptionalDouble.of(contactMinutesPerDay(design));
        double dailyData = dataGbitPerDay(design) + additionalDataGbitPerDay;
        double gbitPerMinute = downlinkMbps * SECONDS_PER_MINUTE / MEGABITS_PER_GIGABIT;
        OptionalDouble dataCapacity = contact.isPresent()
                ? OptionalDouble.of(gbitPerMinute * contact.getAsDouble()) : OptionalDouble.empty();
        double neededMinutes = gbitPerMinute > 0 ? dailyData / gbitPerMinute : 0;
        return Map.of(ResourceType.DATA, ResourceUsage.of(ResourceType.DATA, dailyData, dataCapacity),
                ResourceType.COMMS, ResourceUsage.of(ResourceType.COMMS, neededMinutes,
                        gbitPerMinute > 0 ? contact : OptionalDouble.empty()));
    }


    private static ResourceUsage mass(MissionDesign design) {
        double dryMassKg = dryMassKg(design);
        double used = dryMassKg + propellantKg(design, dryMassKg);
        OptionalDouble capacity = OptionalDouble.empty();
        if (design.launchVehicle() != null && design.orbit() != null) {
            capacity = design.launchVehicle().capacityFor(design.orbit().launchClass());
        }
        return ResourceUsage.of(ResourceType.MASS, used, capacity);
    }

    private static double dryMassKg(MissionDesign design) {
        double dryMassKg = design.instruments().stream().mapToDouble(Instrument::massKg).sum();
        dryMassKg += sum(design.build(), ShipPart::massKg);
        if (design.bus() != null) {
            dryMassKg += design.bus().massKg();
        }
        return dryMassKg;
    }

    // Fuel needs a mission, airframe, and thruster to apply the rocket equation.
    private static double propellantKg(MissionDesign design, double dryMassKg) {
        Optional<ShipPart> thrusters = design.build().part(ModuleSlot.THRUSTERS);
        boolean hasTanks = design.build().part(ModuleSlot.AIRFRAME).isPresent();
        if (design.mission() == null || thrusters.isEmpty() || !hasTanks || dryMassKg <= 0) {
            return 0;
        }
        return FuelMath.propellantKg(dryMassKg, design.mission().deltaVRequiredMs(),
                thrusters.get().specificImpulseS());
    }


    private static ResourceUsage power(MissionDesign design) {
        double used = design.instruments().stream().mapToDouble(Instrument::avgPowerW).sum()
                + sum(design.build(), ShipPart::powerDrawW);
        OptionalDouble capacity = OptionalDouble.empty();
        if (design.build().part(ModuleSlot.POWER).isPresent()) {
            capacity = OptionalDouble.of(sum(design.build(), ShipPart::powerSupplyW));
        } else if (design.bus() != null) {
            capacity = OptionalDouble.of(design.bus().payloadPowerW());
        }
        return ResourceUsage.of(ResourceType.POWER, used, capacity);
    }


    private static ResourceUsage budget(MissionDesign design) {
        double used = design.instruments().stream().mapToDouble(Instrument::costMusd).sum()
                + sum(design.build(), ShipPart::costMusd);
        if (design.bus() != null) {
            used += design.bus().costMusd();
        }
        if (design.launchVehicle() != null) {
            used += design.launchVehicle().costMusd();
        }
        double stationsPerMonth = design.stations().stream().mapToDouble(GroundStation::costMusdPerMonth).sum();
        used += stationsPerMonth * design.scienceMonths();

        OptionalDouble capacity = OptionalDouble.empty();
        if (design.mission() != null) {
            capacity = OptionalDouble.of(design.mission().budgetCapMusd());
        } else if (design.objective() != null) {
            capacity = OptionalDouble.of(design.objective().budgetCapMusd());
        }
        return ResourceUsage.of(ResourceType.BUDGET, used, capacity);
    }


    private static ResourceUsage data(MissionDesign design) {
        OptionalDouble capacity = OptionalDouble.empty();
        if (design.bus() != null && design.orbit() != null) {
            double megabits = design.bus().downlinkMbps() * SECONDS_PER_MINUTE * contactMinutesPerDay(design);
            capacity = OptionalDouble.of(megabits / MEGABITS_PER_GIGABIT);
        }
        return ResourceUsage.of(ResourceType.DATA, dataGbitPerDay(design), capacity);
    }


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


    private static ResourceUsage time(MissionDesign design) {
        double used = buildMonths(design) + design.scienceMonths();
        OptionalDouble capacity = OptionalDouble.empty();
        if (design.mission() != null) {
            capacity = OptionalDouble.of(design.mission().deadlineMonths());
        } else if (design.objective() != null) {
            capacity = OptionalDouble.of(design.objective().deadlineMonths());
        }
        return ResourceUsage.of(ResourceType.TIME, used, capacity);
    }


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
        return design.instruments().stream().mapToDouble(Instrument::dataGbitPerDay).sum()
                + sum(design.build(), ShipPart::dataGbitPerDay);
    }

    // Summing stations can double-count simultaneous visibility.
    private static double contactMinutesPerDay(MissionDesign design) {
        return design.stations().stream()
                .mapToDouble(station -> OrbitMath.contactMinutesPerDay(design.orbit(), station.latitudeDeg()))
                .sum();
    }

    private static double sum(ShipBuild build, ToDoubleFunction<ShipPart> figure) {
        return build.parts().values().stream().mapToDouble(figure).sum();
    }
}
