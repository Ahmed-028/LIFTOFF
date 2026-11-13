package com.liftoff.engine;

import com.liftoff.model.ModuleSlot;
import com.liftoff.model.ShipPart;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Does the cargo fit? Size and mass checks, each giving a BuildIssue when it fails. */
public final class FitRules {

    private FitRules() {
    }

    /**
     * Checks a payload against a cargo bay: length, then width, then mass.
     * Every failure is reported, so the player sees all of them at once. Exactly on the limit fits.
     */
    public static List<BuildIssue> payloadFits(ShipPart bay, ShipPart payload) {
        List<BuildIssue> issues = new ArrayList<>();
        if (payload.lengthM() > bay.bayLengthM()) {
            issues.add(new BuildIssue(BuildIssue.Kind.PAYLOAD_TOO_LONG, ModuleSlot.CARGO_BAY,
                    payload.lengthM(), bay.bayLengthM()));
        }
        if (payload.diameterM() > bay.bayDiameterM()) {
            issues.add(new BuildIssue(BuildIssue.Kind.PAYLOAD_TOO_WIDE, ModuleSlot.CARGO_BAY,
                    payload.diameterM(), bay.bayDiameterM()));
        }
        if (payload.massKg() > bay.payloadMassKg()) {
            issues.add(new BuildIssue(BuildIssue.Kind.PAYLOAD_TOO_HEAVY, ModuleSlot.CARGO_BAY,
                    payload.massKg(), bay.payloadMassKg()));
        }
        return List.copyOf(issues);
    }

    /** Checks the total cargo (bay plus what is in it) against what the airframe can carry. */
    public static Optional<BuildIssue> airframeCarries(ShipPart airframe, double cargoKg) {
        if (cargoKg > airframe.payloadMassKg()) {
            return Optional.of(new BuildIssue(BuildIssue.Kind.PAYLOAD_TOO_HEAVY, ModuleSlot.AIRFRAME,
                    cargoKg, airframe.payloadMassKg()));
        }
        return Optional.empty();
    }
}
