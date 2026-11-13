package com.liftoff.session.stub;

import java.util.ArrayList;
import java.util.List;

/** Checks ship fit, power and propellant for the mission's build review. */
public final class BuildCalculator {

    private static final double STANDARD_GRAVITY = 9.80665;

    private BuildCalculator() {
    }

    /** {@code mission} may be null while no mission is selected. */
    public static BuildReport calculate(ShipBuild build, Mission mission) {
        return calculate(build, mission, 0, 0, 0, 0);
    }

    public static BuildReport calculate(ShipBuild build, Mission mission, double instrumentMassKg,
                                        double instrumentPowerW, double suppliesKg, double requiredSuppliesKg) {
        List<BuildIssue> issues = new ArrayList<>();
        for (ModuleSlot slot : ModuleSlot.values()) {
            if (build.part(slot).isEmpty()) {
                issues.add(new BuildIssue(BuildIssue.Kind.SLOT_EMPTY, slot, 1, 0));
            }
        }
        double dryMassKg = build.parts().values().stream().mapToDouble(ShipPart::massKg).sum();
        double costMusd = build.parts().values().stream().mapToDouble(ShipPart::costMusd).sum();
        double powerSupplyW = build.parts().values().stream().mapToDouble(ShipPart::powerSupplyW).sum();
        double powerDrawW = build.parts().values().stream().mapToDouble(ShipPart::powerDrawW).sum();
        double deltaVRequiredMs = mission == null ? 0 : mission.deltaVRequiredMs();
        ShipPart frame = build.parts().get(ModuleSlot.AIRFRAME);
        ShipPart bay = build.parts().get(ModuleSlot.CARGO_BAY);
        ShipPart payload = build.parts().get(ModuleSlot.SATELLITE);
        if (payload != null && bay != null) {
            check(issues, BuildIssue.Kind.PAYLOAD_TOO_LONG, ModuleSlot.CARGO_BAY, payload.lengthM(), bay.bayLengthM());
            check(issues, BuildIssue.Kind.PAYLOAD_TOO_WIDE, ModuleSlot.CARGO_BAY, payload.diameterM(), bay.bayDiameterM());
            check(issues, BuildIssue.Kind.PAYLOAD_TOO_HEAVY, ModuleSlot.CARGO_BAY, payload.massKg(), bay.payloadMassKg());
        }
        if (frame != null && payload != null) {
            check(issues, BuildIssue.Kind.PAYLOAD_TOO_HEAVY, ModuleSlot.AIRFRAME, payload.massKg(), frame.payloadMassKg());
        }
        if (build.parts().containsKey(ModuleSlot.POWER)) {
            check(issues, BuildIssue.Kind.POWER_SHORT, ModuleSlot.POWER, powerDrawW + instrumentPowerW, powerSupplyW);
        }
        check(issues, BuildIssue.Kind.SUPPLIES_SHORT, null, requiredSuppliesKg, suppliesKg);

        double fuelKg = 0;
        double deltaVAvailableMs = 0;
        ShipPart thrusters = build.parts().get(ModuleSlot.THRUSTERS);
        if (deltaVRequiredMs > 0 && build.isComplete() && thrusters.specificImpulseS() > 0) {
            double exhaustVelocity = STANDARD_GRAVITY * thrusters.specificImpulseS();
            double surfaceDeltaV = mission.surfaceDeltaVRequiredMs();
            double transitDeltaV = deltaVRequiredMs - surfaceDeltaV;
            double flightMass = dryMassKg + instrumentMassKg + suppliesKg;
            double surfaceMargin = Double.POSITIVE_INFINITY;
            if (surfaceDeltaV > 0) {
                ShipPart surfaceStage = mission.destination() == Mission.Destination.MOON ? payload : bay;
                double surfaceMass = payload.massKg() + instrumentMassKg + suppliesKg;
                if (surfaceStage == bay) {
                    surfaceMass += bay.massKg() + build.parts().get(ModuleSlot.POWER).massKg();
                }
                fuelKg = surfaceMass * Math.expm1(surfaceDeltaV / exhaustVelocity);
                check(issues, BuildIssue.Kind.FUEL_SHORT, surfaceStage.slot(), fuelKg, surfaceStage.propellantCapacityKg());
                surfaceMargin = exhaustVelocity * Math.log1p(surfaceStage.propellantCapacityKg() / surfaceMass)
                        / surfaceDeltaV;
            }
            double transitMass = flightMass + fuelKg;
            double transitFuel = transitMass * Math.expm1(transitDeltaV / exhaustVelocity);
            check(issues, BuildIssue.Kind.FUEL_SHORT, ModuleSlot.AIRFRAME, transitFuel, frame.propellantCapacityKg());
            fuelKg += transitFuel;
            double transitMargin = transitDeltaV == 0 ? Double.POSITIVE_INFINITY
                    : exhaustVelocity * Math.log1p(frame.propellantCapacityKg() / transitMass) / transitDeltaV;
            deltaVAvailableMs = deltaVRequiredMs * Math.min(surfaceMargin, transitMargin);
        }
        return new BuildReport(dryMassKg, fuelKg, dryMassKg + fuelKg + suppliesKg, costMusd,
                powerSupplyW, powerDrawW + instrumentPowerW, deltaVAvailableMs, deltaVRequiredMs, issues);
    }

    private static void check(List<BuildIssue> issues, BuildIssue.Kind kind, ModuleSlot slot,
                              double needed, double available) {
        if (needed > available) {
            issues.add(new BuildIssue(kind, slot, needed, available));
        }
    }
}
