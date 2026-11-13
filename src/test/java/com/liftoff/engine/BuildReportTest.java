package com.liftoff.engine;

import com.liftoff.model.ModuleSlot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildReportTest {

    private static BuildReport report(List<BuildIssue> issues) {
        return new BuildReport(1000, 500, 1500, 200, 300, 250, 2000, 1500, issues);
    }

    @Test
    void canConfirmWhenNothingIsWrong() {
        assertTrue(report(List.of()).canConfirm());
    }

    @Test
    void cannotConfirmWithEvenOneIssue() {
        BuildIssue shortOfPower = new BuildIssue(BuildIssue.Kind.POWER_SHORT, ModuleSlot.POWER, 160, 115);
        assertFalse(report(List.of(shortOfPower)).canConfirm());
    }

    @Test
    void issueListCannotBeChangedFromOutside() {
        List<BuildIssue> issues = new ArrayList<>();
        BuildReport r = report(issues);
        issues.add(new BuildIssue(BuildIssue.Kind.SLOT_EMPTY, ModuleSlot.POWER, 0, 0));
        assertTrue(r.issues().isEmpty());
        assertTrue(r.canConfirm());
    }

    @Test
    void issueListReturnedCannotBeEdited() {
        BuildReport r = report(List.of());
        assertThrows(UnsupportedOperationException.class,
                () -> r.issues().add(new BuildIssue(BuildIssue.Kind.SLOT_EMPTY, ModuleSlot.POWER, 0, 0)));
    }

    @Test
    void issueMustSayWhatKindItIs() {
        assertThrows(NullPointerException.class, () -> new BuildIssue(null, ModuleSlot.POWER, 0, 0));
    }

    @Test
    void budgetIssueBelongsToNoSlot() {
        // Over budget is about the whole ship, so slot may be null.
        BuildIssue overBudget = new BuildIssue(BuildIssue.Kind.OVER_BUDGET, null, 470, 450);
        assertEquals(470, overBudget.needed());
        assertEquals(450, overBudget.available());
    }

    @Test
    void kindsMatchTheWarningsInDocument05PlusCrewSupplies() {
        assertEquals(List.of("SLOT_EMPTY", "PAYLOAD_TOO_HEAVY", "PAYLOAD_TOO_LONG", "PAYLOAD_TOO_WIDE",
                        "POWER_SHORT", "FUEL_SHORT", "OVER_BUDGET", "SUPPLIES_SHORT"),
                List.of(BuildIssue.Kind.values()).stream().map(Enum::name).toList());
    }

    @Test
    void fuelLevelFollowsTheSameBandsAsTheOtherGauges() {
        assertEquals(ResourceLevel.OK, new BuildReport(1, 0, 1, 0, 0, 0, 0, 0, List.of()).fuelLevel());
        assertEquals(ResourceLevel.OK, new BuildReport(1, 1, 2, 0, 0, 0, 2000, 1500, List.of()).fuelLevel());
        assertEquals(ResourceLevel.TIGHT, new BuildReport(1, 1, 2, 0, 0, 0, 1600, 1500, List.of()).fuelLevel());
        assertEquals(ResourceLevel.OVER, new BuildReport(1, 1, 2, 0, 0, 0, 1000, 1500, List.of()).fuelLevel());
        BuildIssue empty = new BuildIssue(BuildIssue.Kind.SLOT_EMPTY, ModuleSlot.POWER, 0, 0);
        assertEquals(ResourceLevel.UNKNOWN, new BuildReport(1, 0, 1, 0, 0, 0, 0, 1500, List.of(empty)).fuelLevel());
    }
}
