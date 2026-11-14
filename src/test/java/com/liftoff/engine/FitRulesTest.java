package com.liftoff.engine;

import com.liftoff.TestParts;
import com.liftoff.model.ModuleSlot;
import com.liftoff.model.ShipPart;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FitRulesTest {


    private static final ShipPart BAY = TestParts.cargoBay("bay", 50, 300, 3, 2, 5);

    @Test
    void aPayloadThatFitsRaisesNothing() {
        ShipPart satellite = TestParts.satellite("sat", 100, 2, 1, 100, 50);
        assertTrue(FitRules.payloadFits(BAY, satellite).isEmpty());
    }

    @Test
    void aPayloadExactlyAsBigAsTheBayFits() {
        ShipPart satellite = TestParts.satellite("snug", 300, 3, 2, 100, 50);
        assertTrue(FitRules.payloadFits(BAY, satellite).isEmpty());
    }

    @Test
    void tooLong() {
        ShipPart satellite = TestParts.satellite("long", 100, 4, 1, 100, 50);
        assertEquals(List.of(new BuildIssue(BuildIssue.Kind.PAYLOAD_TOO_LONG, ModuleSlot.CARGO_BAY, 4, 3)),
                FitRules.payloadFits(BAY, satellite));
    }

    @Test
    void tooWide() {
        ShipPart satellite = TestParts.satellite("wide", 100, 2, 2.5, 100, 50);
        assertEquals(List.of(new BuildIssue(BuildIssue.Kind.PAYLOAD_TOO_WIDE, ModuleSlot.CARGO_BAY, 2.5, 2)),
                FitRules.payloadFits(BAY, satellite));
    }

    @Test
    void tooHeavyForTheBay() {
        ShipPart satellite = TestParts.satellite("heavy", 350, 2, 1, 100, 50);
        assertEquals(List.of(new BuildIssue(BuildIssue.Kind.PAYLOAD_TOO_HEAVY, ModuleSlot.CARGO_BAY, 350, 300)),
                FitRules.payloadFits(BAY, satellite));
    }

    @Test
    void everyFailureIsReportedAtOnce() {
        ShipPart satellite = TestParts.satellite("huge", 350, 4, 2.5, 100, 50);
        List<BuildIssue.Kind> kinds = FitRules.payloadFits(BAY, satellite).stream().map(BuildIssue::kind).toList();
        assertEquals(List.of(BuildIssue.Kind.PAYLOAD_TOO_LONG, BuildIssue.Kind.PAYLOAD_TOO_WIDE,
                BuildIssue.Kind.PAYLOAD_TOO_HEAVY), kinds);
    }

    @Test
    void airframeCarriesCargoUpToItsLimit() {
        ShipPart airframe = TestParts.airframe("plane", 1000, 500, 2000, 50, 100);
        assertEquals(Optional.empty(), FitRules.airframeCarries(airframe, 150));
        assertEquals(Optional.empty(), FitRules.airframeCarries(airframe, 500));
    }

    @Test
    void cargoTooHeavyForTheAirframe() {
        ShipPart airframe = TestParts.airframe("plane", 1000, 120, 2000, 50, 100);
        assertEquals(Optional.of(new BuildIssue(BuildIssue.Kind.PAYLOAD_TOO_HEAVY, ModuleSlot.AIRFRAME, 150, 120)),
                FitRules.airframeCarries(airframe, 150));
    }
}
