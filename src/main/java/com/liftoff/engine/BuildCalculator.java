package com.liftoff.engine;

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
 * empty tabs (tab order), cargo fit, power, fuel, budget.
 */
public final class BuildCalculator {

    private BuildCalculator() {
    }

    public static BuildReport calculate(ShipBuild build, Mission mission) {
        Optional<ShipPart> airframe = build.part(ModuleSlot.AIRFRAME);
        Optional<ShipPart> thrusters = build.part(ModuleSlot.THRUSTERS);
        Optional<ShipPart> power = build.part(ModuleSlot.POWER);
        Optional<ShipPart> bay = build.part(ModuleSlot.CARGO_BAY);
        Optional<ShipPart> payload = build.part(ModuleSlot.SATELLITE);

        double dryMassKg = sum(build, ShipPart::massKg);
        double costMusd = sum(build, ShipPart::costMusd);
        double powerSupplyW = sum(build, ShipPart::powerSupplyW);
        double powerDrawW = sum(build, ShipPart::powerDrawW);

        List<BuildIssue> issues = new ArrayList<>();

        // 1. A tab with nothing chosen.
        for (ModuleSlot slot : ModuleSlot.values()) {
            if (build.part(slot).isEmpty()) {
                issues.add(new BuildIssue(BuildIssue.Kind.SLOT_EMPTY, slot, 0, 0));
            }
        }

        // 2. Does the payload fit the bay, and can the airframe carry bay plus payload?
        if (bay.isPresent() && payload.isPresent()) {
            issues.addAll(FitRules.payloadFits(bay.get(), payload.get()));
        }
        if (airframe.isPresent()) {
            double cargoKg = bay.map(ShipPart::massKg).orElse(0.0) + payload.map(ShipPart::massKg).orElse(0.0);
            FitRules.airframeCarries(airframe.get(), cargoKg).ifPresent(issues::add);
        }

        // 3. Power: everything that draws, against what the power system gives.
        if (power.isPresent() && powerDrawW > powerSupplyW) {
            issues.add(new BuildIssue(BuildIssue.Kind.POWER_SHORT, ModuleSlot.POWER, powerDrawW, powerSupplyW));
        }

        // 4. Fuel: the rocket equation with the thruster's Isp, against the airframe's tanks.
        double propellantKg = 0;
        double deltaVAvailableMs = 0;
        if (airframe.isPresent() && thrusters.isPresent() && dryMassKg > 0) {
            double specificImpulseS = thrusters.get().specificImpulseS();
            double tankKg = airframe.get().propellantCapacityKg();
            propellantKg = FuelMath.propellantKg(dryMassKg, mission.deltaVRequiredMs(), specificImpulseS);
            deltaVAvailableMs = FuelMath.deltaVMs(dryMassKg, tankKg, specificImpulseS);
            if (propellantKg > tankKg) {
                issues.add(new BuildIssue(BuildIssue.Kind.FUEL_SHORT, ModuleSlot.AIRFRAME, propellantKg, tankKg));
            }
        }

        // 5. Budget: about the whole ship, so it belongs to no tab.
        if (costMusd > mission.budgetCapMusd()) {
            issues.add(new BuildIssue(BuildIssue.Kind.OVER_BUDGET, null, costMusd, mission.budgetCapMusd()));
        }

        return new BuildReport(dryMassKg, propellantKg, dryMassKg + propellantKg, costMusd,
                powerSupplyW, powerDrawW, deltaVAvailableMs, mission.deltaVRequiredMs(), issues);
    }

    private static double sum(ShipBuild build, ToDoubleFunction<ShipPart> figure) {
        return build.parts().values().stream().mapToDouble(figure).sum();
    }
}
