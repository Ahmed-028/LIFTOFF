package com.liftoff.engine;

import java.util.List;

/** Everything the builder screen shows about the current ship. Made by BuildCalculator. */
public record BuildReport(
        double dryMassKg,          // all parts, no propellant
        double propellantKg,       // propellant the trip needs (rocket equation)
        double totalMassKg,        // dry + propellant
        double costMusd,
        double powerSupplyW,
        double powerDrawW,
        double deltaVAvailableMs,  // speed change the full tanks can give
        double deltaVRequiredMs,   // speed change the mission needs
        List<BuildIssue> issues) {

    public BuildReport {
        issues = List.copyOf(issues);
    }

    /** Confirm Build is allowed only when nothing is wrong. */
    public boolean canConfirm() {
        return issues.isEmpty();
    }

    /** The fuel gauge's state: how the speed change the trip needs compares with what the tanks can give. */
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
