package com.liftoff.session.stub;

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
}
