package com.liftoff.session.stub;

import com.liftoff.engine.GameRules;
import com.liftoff.engine.ResourceLevel;

import java.util.List;

/** TODO(Member 2): stub of engine/BuildReport — everything the builder screen shows. */
public record BuildReport(double dryMassKg, double propellantKg, double totalMassKg,
                          double costMusd, double powerSupplyW, double powerDrawW,
                          double deltaVAvailableMs, double deltaVRequiredMs,
                          List<BuildIssue> issues) {

    public BuildReport {
        issues = List.copyOf(issues);
    }

    public boolean canConfirm() {
        return issues.isEmpty();
    }

    public ResourceLevel fuelLevel() {
        if (deltaVRequiredMs <= 0) {
            return ResourceLevel.OK;
        }
        if (issues.stream().anyMatch(issue -> issue.kind() == BuildIssue.Kind.SLOT_EMPTY)) {
            return ResourceLevel.UNKNOWN;
        }
        if (deltaVAvailableMs < deltaVRequiredMs) {
            return ResourceLevel.OVER;
        }
        return deltaVRequiredMs >= GameRules.TIGHT_FRACTION * deltaVAvailableMs
                ? ResourceLevel.TIGHT : ResourceLevel.OK;
    }
}
