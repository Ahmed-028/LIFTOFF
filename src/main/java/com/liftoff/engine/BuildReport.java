package com.liftoff.engine;

import java.util.List;


public record BuildReport(
        double dryMassKg,
        double propellantKg,       // rocket-equation propellant required
        double totalMassKg,
        double costMusd,
        double powerSupplyW,
        double powerDrawW,
        double deltaVAvailableMs,
        double deltaVRequiredMs,
        List<BuildIssue> issues) {

    public BuildReport {
        issues = List.copyOf(issues);
    }


    public boolean canConfirm() {
        return issues.isEmpty();
    }
}
