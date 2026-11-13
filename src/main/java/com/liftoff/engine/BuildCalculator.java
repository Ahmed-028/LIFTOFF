package com.liftoff.engine;

import com.liftoff.model.Destination;
import com.liftoff.model.Mission;
import com.liftoff.model.ModuleSlot;
import com.liftoff.model.ShipBuild;
import com.liftoff.model.ShipPart;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.ToDoubleFunction;

/**
 * Works out mass, cost, power, fuel and every warning for a ship. Pure: same input, same report.
 *
 * <p>Works on an unfinished ship too: each empty tab gives one SLOT_EMPTY, and a check that needs
 * a missing part is skipped rather than reported twice. Issues come out in a fixed order:
 * empty tabs (tab order), cargo fit, power, supplies, fuel, budget.
 *
 * <p>Fuel is flown in up to two legs (docs/data-sources.md, "Deep-space flight assumptions"). The
 * surface leg, if the mission has one, is burned from the tanks of the surface stage: the lander
 * (SATELLITE part) on the Moon, the entry stage (CARGO_BAY part) on Mars. The rest of the trip is
 * burned from the airframe's tanks and pushes the whole ship, the surface stage's propellant
 * included. The chosen thruster's specific impulse is used for both legs.
 */
public final class BuildCalculator {

    private BuildCalculator() {
    }

    /** The ship alone: no instruments, no crew supplies, no other costs. */
    public static BuildReport calculate(ShipBuild build, Mission mission) {
        return calculate(build, mission, BuildLoad.NONE);
    }

    /** @param mission the mission flown, or null while none is selected: no trip and no budget cap then */
    public static BuildReport calculate(ShipBuild build, Mission mission, BuildLoad load) {
        Optional<ShipPart> airframe = build.part(ModuleSlot.AIRFRAME);
        Optional<ShipPart> power = build.part(ModuleSlot.POWER);
        Optional<ShipPart> bay = build.part(ModuleSlot.CARGO_BAY);
        Optional<ShipPart> payload = build.part(ModuleSlot.SATELLITE);

        double dryMassKg = sum(build, ShipPart::massKg);
        double costMusd = sum(build, ShipPart::costMusd);
        double powerSupplyW = sum(build, ShipPart::powerSupplyW);
        double powerDrawW = sum(build, ShipPart::powerDrawW) + load.instrumentPowerW();
        double deltaVRequiredMs = mission == null ? 0 : mission.deltaVRequiredMs();

        List<BuildIssue> issues = new ArrayList<>();

        // 1. A tab with nothing chosen.
        for (ModuleSlot slot : ModuleSlot.values()) {
            if (build.part(slot).isEmpty()) {
                issues.add(new BuildIssue(BuildIssue.Kind.SLOT_EMPTY, slot, 0, 0));
            }
        }

        // 2. Does the payload fit the bay, and can the airframe carry it?
        if (bay.isPresent() && payload.isPresent()) {
            issues.addAll(FitRules.payloadFits(bay.get(), payload.get()));
        }
        if (airframe.isPresent() && payload.isPresent()) {
            FitRules.airframeCarries(airframe.get(), payload.get().massKg()).ifPresent(issues::add);
        }

        // 3. Power: everything that draws, against what the power system gives.
        if (power.isPresent() && powerDrawW > powerSupplyW) {
            issues.add(new BuildIssue(BuildIssue.Kind.POWER_SHORT, ModuleSlot.POWER, powerDrawW, powerSupplyW));
        }

        // 4. Crew supplies: what is packed, against what the crew needs for the whole trip.
        if (load.requiredSuppliesKg() > load.suppliesKg()) {
            issues.add(new BuildIssue(BuildIssue.Kind.SUPPLIES_SHORT, null,
                    load.requiredSuppliesKg(), load.suppliesKg()));
        }

        // 5. Fuel: the rocket equation with the thruster's Isp, against the tanks of each stage.
        Fuel fuel = fuel(build, mission, load, dryMassKg, issues);

        // 6. Budget: about the whole mission, so it belongs to no tab.
        double totalCostMusd = costMusd + load.otherCostMusd();
        if (mission != null && totalCostMusd > mission.budgetCapMusd()) {
            issues.add(new BuildIssue(BuildIssue.Kind.OVER_BUDGET, null, totalCostMusd, mission.budgetCapMusd()));
        }

        return new BuildReport(dryMassKg, fuel.propellantKg(), dryMassKg + fuel.propellantKg() + load.suppliesKg(),
                costMusd, powerSupplyW, powerDrawW, fuel.deltaVAvailableMs(), deltaVRequiredMs, issues);
    }

    /** Propellant for the whole trip and the speed change the full tanks can give. */
    private record Fuel(double propellantKg, double deltaVAvailableMs) {
        static final Fuel NONE = new Fuel(0, 0);
    }

    /** Propellant for one leg and how far the stage's tanks reach, as a multiple of that leg. */
    private record Leg(double propellantKg, double reach) {
        static final Leg NONE = new Leg(0, Double.POSITIVE_INFINITY);
    }

    /**
     * Burns the surface leg (if any) from the surface stage's tanks, then the rest of the trip from
     * the airframe's tanks, pushing the whole ship and the surface propellant. Adds a FUEL_SHORT
     * for each stage whose tanks are too small. Nothing to work out until the parts it needs are chosen.
     */
    private static Fuel fuel(ShipBuild build, Mission mission, BuildLoad load, double dryMassKg,
                             List<BuildIssue> issues) {
        double deltaVRequiredMs = mission == null ? 0 : mission.deltaVRequiredMs();
        double surfaceDeltaVMs = mission == null ? 0 : mission.surfaceDeltaVRequiredMs();
        double transitDeltaVMs = deltaVRequiredMs - surfaceDeltaVMs;
        double flightMassKg = dryMassKg + load.instrumentMassKg() + load.suppliesKg();
        boolean ready = surfaceDeltaVMs > 0 ? build.isComplete()
                : build.part(ModuleSlot.AIRFRAME).isPresent() && build.part(ModuleSlot.THRUSTERS).isPresent();
        if (!ready || flightMassKg <= 0) {
            return Fuel.NONE;
        }
        double specificImpulseS = build.part(ModuleSlot.THRUSTERS).orElseThrow().specificImpulseS();
        Leg surface = surfaceDeltaVMs > 0
                ? surfaceLeg(build, mission, load, surfaceDeltaVMs, specificImpulseS, issues) : Leg.NONE;

        double pushedMassKg = flightMassKg + surface.propellantKg();
        double tankKg = build.part(ModuleSlot.AIRFRAME).orElseThrow().propellantCapacityKg();
        double transitKg = FuelMath.propellantKg(pushedMassKg, transitDeltaVMs, specificImpulseS);
        double transitReachMs = FuelMath.deltaVMs(pushedMassKg, tankKg, specificImpulseS);
        if (transitKg > tankKg) {
            issues.add(new BuildIssue(BuildIssue.Kind.FUEL_SHORT, ModuleSlot.AIRFRAME, transitKg, tankKg));
        }
        double propellantKg = surface.propellantKg() + transitKg;
        if (surfaceDeltaVMs <= 0) {
            return new Fuel(propellantKg, transitReachMs);
        }
        double transitShare = transitDeltaVMs > 0 ? transitReachMs / transitDeltaVMs : Double.POSITIVE_INFINITY;
        return new Fuel(propellantKg, deltaVRequiredMs * Math.min(surface.reach(), transitShare));
    }

    /**
     * The landing leg, burned by the surface stage: the lander (payload) on the Moon, the entry
     * stage (cargo bay) on Mars, which also lands the bay and the power system.
     */
    private static Leg surfaceLeg(ShipBuild build, Mission mission, BuildLoad load, double surfaceDeltaVMs,
                                  double specificImpulseS, List<BuildIssue> issues) {
        ShipPart payload = build.part(ModuleSlot.SATELLITE).orElseThrow();
        ShipPart bay = build.part(ModuleSlot.CARGO_BAY).orElseThrow();
        boolean moon = mission.destination() == Destination.MOON_SURFACE;
        ShipPart stage = moon ? payload : bay;
        double landedMassKg = payload.massKg() + load.instrumentMassKg() + load.suppliesKg();
        if (!moon) {
            landedMassKg += bay.massKg() + build.part(ModuleSlot.POWER).orElseThrow().massKg();
        }
        if (landedMassKg <= 0) {
            return Leg.NONE;
        }
        double tankKg = stage.propellantCapacityKg();
        double propellantKg = FuelMath.propellantKg(landedMassKg, surfaceDeltaVMs, specificImpulseS);
        if (propellantKg > tankKg) {
            issues.add(new BuildIssue(BuildIssue.Kind.FUEL_SHORT, stage.slot(), propellantKg, tankKg));
        }
        return new Leg(propellantKg, FuelMath.deltaVMs(landedMassKg, tankKg, specificImpulseS) / surfaceDeltaVMs);
    }

    private static double sum(ShipBuild build, ToDoubleFunction<ShipPart> figure) {
        return build.parts().values().stream().mapToDouble(figure).sum();
    }
}
