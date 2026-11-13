package com.liftoff.engine;

import com.liftoff.model.Capability;
import com.liftoff.model.Instrument;
import com.liftoff.model.MissionDesign;
import com.liftoff.model.Requirement;

import java.util.ArrayList;
import java.util.List;

/**
 * For the Review step: can this design answer the mission's science question?
 * Checks that every required measurement is made sharply enough, and that the orbit flies over
 * the target latitude. Pure: same design in, same list out. Measurement issues come first.
 */
public final class DesignValidator {

    private DesignValidator() {
    }

    public static List<DesignIssue> validate(MissionDesign design) {
        List<Requirement> required;
        double targetLatitudeDeg;
        if (design.mission() != null) {
            required = design.mission().required();
            targetLatitudeDeg = design.mission().targetLatitudeDeg();
        } else if (design.objective() != null) {
            required = design.objective().required();
            targetLatitudeDeg = design.objective().targetLatitudeDeg();
        } else {
            return List.of();
        }

        List<DesignIssue> issues = new ArrayList<>();
        for (Requirement requirement : required) {
            checkMeasurement(requirement, design.instruments(), issues);
        }
        if (design.orbit() != null) {
            // An orbit flies over latitudes up to its inclination (or 180 minus it), north and south alike.
            double needed = Math.abs(targetLatitudeDeg);
            double reached = OrbitMath.maxLatitudeDeg(design.orbit().inclinationDeg());
            if (needed > reached) {
                issues.add(new DesignIssue(DesignIssue.Kind.ORBIT_MISSES_TARGET, null, needed, reached));
            }
        }
        return List.copyOf(issues);
    }

    private static void checkMeasurement(Requirement requirement, List<Instrument> instruments,
                                         List<DesignIssue> issues) {
        List<Capability> matching = instruments.stream()
                .flatMap(instrument -> instrument.capabilities().stream())
                .filter(capability -> capability.type() == requirement.type())
                .toList();
        Double limitM = requirement.maxResolutionM();

        if (matching.isEmpty()) {
            issues.add(new DesignIssue(DesignIssue.Kind.MEASUREMENT_MISSING, requirement.type(),
                    limitM == null ? 0 : limitM, 0));
            return;
        }
        if (limitM == null) {
            return; // any resolution will do
        }
        // Finest stated detail. A capability whose source gives no resolution cannot prove the goal is met.
        double finestM = matching.stream()
                .filter(capability -> capability.resolutionM() != null)
                .mapToDouble(Capability::resolutionM)
                .min()
                .orElse(Double.NaN);
        if (!(finestM <= limitM)) {
            issues.add(new DesignIssue(DesignIssue.Kind.RESOLUTION_TOO_COARSE, requirement.type(), limitM, finestM));
        }
    }
}
