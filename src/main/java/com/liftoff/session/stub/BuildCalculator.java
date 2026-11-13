package com.liftoff.session.stub;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO(Member 2): stub of engine/BuildCalculator. It only reports empty slots and adds up the
 * parts' own figures; fit, power, fuel and budget checks are the real calculator's job.
 */
public final class BuildCalculator {

    private BuildCalculator() {
    }

    /** {@code mission} may be null while no mission is selected. */
    public static BuildReport calculate(ShipBuild build, Mission mission) {
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
        return new BuildReport(dryMassKg, 0, dryMassKg, costMusd, powerSupplyW, powerDrawW,
                0, deltaVRequiredMs, issues);
    }
}
