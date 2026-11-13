package com.liftoff.engine;

import com.liftoff.SampleData;
import com.liftoff.TestParts;
import com.liftoff.model.Mission;
import com.liftoff.model.ModuleSlot;
import com.liftoff.model.ShipBuild;
import com.liftoff.model.ShipPart;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round numbers, so every expected value can be checked by eye.
 *
 * <pre>
 * part      mass   other
 * airframe  1000   carries 500 kg, tanks 2000 kg, draws 50 W, $100M
 * thruster   100   Isp 300 s, $10M
 * power       50   gives 200 W, $20M
 * bay         50   3 m x 2 m inside, carries 300 kg, $5M
 * satellite  100   2 m x 1 m, draws 100 W, $50M
 * ---------------
 * dry mass  1300   cost $185M, draw 150 W of 200 W, cargo 150 kg of 500 kg
 * </pre>
 *
 * The mission's delta-v is Isp x g0 x ln 2, so the trip needs propellant equal to the dry mass: 1300 kg.
 */
class BuildCalculatorTest {

    private static final double TINY = 1e-6;
    private static final double ISP = 300;
    private static final double DELTA_V = ISP * 9.80665 * Math.log(2);

    private static final ShipPart AIRFRAME = TestParts.airframe("plane", 1000, 500, 2000, 50, 100);
    private static final ShipPart THRUSTER = TestParts.thruster("engine", 100, ISP, 0, 10);
    private static final ShipPart POWER = TestParts.power("solar", 50, 200, 20);
    private static final ShipPart BAY = TestParts.cargoBay("bay", 50, 300, 3, 2, 5);
    private static final ShipPart SATELLITE = TestParts.satellite("sat", 100, 2, 1, 100, 50);

    private static final Mission MISSION = TestParts.mission("trip", 200, DELTA_V);

    private static ShipBuild goodBuild() {
        return ShipBuild.empty().with(AIRFRAME).with(THRUSTER).with(POWER).with(BAY).with(SATELLITE);
    }

    private static List<BuildIssue> issues(ShipBuild build, Mission mission) {
        return BuildCalculator.calculate(build, mission).issues();
    }

    // ---- the numbers ----------------------------------------------------------------------

    @Test
    void addsUpMassCostAndPower() {
        BuildReport report = BuildCalculator.calculate(goodBuild(), MISSION);
        assertEquals(1300, report.dryMassKg(), TINY);
        assertEquals(185, report.costMusd(), TINY);
        assertEquals(200, report.powerSupplyW(), TINY);
        assertEquals(150, report.powerDrawW(), TINY);
    }

    @Test
    void worksOutFuelFromTheRocketEquation() {
        BuildReport report = BuildCalculator.calculate(goodBuild(), MISSION);
        assertEquals(1300, report.propellantKg(), TINY);       // equal to dry mass, by choice of delta-v
        assertEquals(2600, report.totalMassKg(), TINY);        // dry + propellant
        assertEquals(DELTA_V, report.deltaVRequiredMs(), TINY);
        // Full tanks (2000 kg) could give Isp x g0 x ln((1300 + 2000) / 1300).
        assertEquals(ISP * 9.80665 * Math.log(3300.0 / 1300.0), report.deltaVAvailableMs(), TINY);
    }

    @Test
    void aGoodBuildCanBeConfirmed() {
        BuildReport report = BuildCalculator.calculate(goodBuild(), MISSION);
        assertEquals(List.of(), report.issues());
        assertTrue(report.canConfirm());
    }

    @Test
    void sameInputGivesTheSameReport() {
        assertEquals(BuildCalculator.calculate(goodBuild(), MISSION), BuildCalculator.calculate(goodBuild(), MISSION));
    }

    // ---- one test per kind of issue -------------------------------------------------------

    @Test
    void slotEmpty_anEmptyBuildAsksForEveryTabInTabOrder() {
        BuildReport report = BuildCalculator.calculate(ShipBuild.empty(), MISSION);
        assertEquals(List.of(
                        new BuildIssue(BuildIssue.Kind.SLOT_EMPTY, ModuleSlot.AIRFRAME, 0, 0),
                        new BuildIssue(BuildIssue.Kind.SLOT_EMPTY, ModuleSlot.THRUSTERS, 0, 0),
                        new BuildIssue(BuildIssue.Kind.SLOT_EMPTY, ModuleSlot.POWER, 0, 0),
                        new BuildIssue(BuildIssue.Kind.SLOT_EMPTY, ModuleSlot.CARGO_BAY, 0, 0),
                        new BuildIssue(BuildIssue.Kind.SLOT_EMPTY, ModuleSlot.SATELLITE, 0, 0)),
                report.issues());
        assertFalse(report.canConfirm());
        assertEquals(0, report.dryMassKg());
        assertEquals(0, report.propellantKg());
        assertEquals(0, report.deltaVAvailableMs());
    }

    @Test
    void payloadTooLong() {
        ShipBuild build = goodBuild().with(TestParts.satellite("long", 100, 4, 1, 100, 50));
        assertEquals(List.of(new BuildIssue(BuildIssue.Kind.PAYLOAD_TOO_LONG, ModuleSlot.CARGO_BAY, 4, 3)),
                issues(build, MISSION));
    }

    @Test
    void payloadTooWide() {
        ShipBuild build = goodBuild().with(TestParts.satellite("wide", 100, 2, 2.5, 100, 50));
        assertEquals(List.of(new BuildIssue(BuildIssue.Kind.PAYLOAD_TOO_WIDE, ModuleSlot.CARGO_BAY, 2.5, 2)),
                issues(build, MISSION));
    }

    @Test
    void payloadTooHeavyForTheBay() {
        // 350 kg in a bay rated for 300 kg; the airframe (500 kg) can still carry bay + satellite = 400 kg.
        ShipBuild build = goodBuild().with(TestParts.satellite("heavy", 350, 2, 1, 100, 50));
        assertEquals(List.of(new BuildIssue(BuildIssue.Kind.PAYLOAD_TOO_HEAVY, ModuleSlot.CARGO_BAY, 350, 300)),
                issues(build, MISSION));
    }

    @Test
    void payloadTooHeavyForTheAirframe() {
        // Cargo is bay + satellite = 150 kg; this airframe carries 120 kg.
        ShipBuild build = goodBuild().with(TestParts.airframe("weak", 1000, 120, 2000, 50, 100));
        assertEquals(List.of(new BuildIssue(BuildIssue.Kind.PAYLOAD_TOO_HEAVY, ModuleSlot.AIRFRAME, 150, 120)),
                issues(build, MISSION));
    }

    @Test
    void powerShort() {
        ShipBuild build = goodBuild().with(TestParts.power("dim", 50, 100, 20));
        assertEquals(List.of(new BuildIssue(BuildIssue.Kind.POWER_SHORT, ModuleSlot.POWER, 150, 100)),
                issues(build, MISSION));
    }

    @Test
    void fuelShort() {
        // The trip needs 1300 kg; these tanks hold 1000 kg.
        ShipBuild build = goodBuild().with(TestParts.airframe("small-tanks", 1000, 500, 1000, 50, 100));
        List<BuildIssue> issues = issues(build, MISSION);
        assertEquals(1, issues.size());
        BuildIssue issue = issues.get(0);
        assertEquals(BuildIssue.Kind.FUEL_SHORT, issue.kind());
        assertEquals(ModuleSlot.AIRFRAME, issue.slot());
        assertEquals(1300, issue.needed(), TINY);
        assertEquals(1000, issue.available(), TINY);
    }

    @Test
    void overBudget() {
        Mission tight = TestParts.mission("tight", 150, DELTA_V);
        assertEquals(List.of(new BuildIssue(BuildIssue.Kind.OVER_BUDGET, null, 185, 150)),
                issues(goodBuild(), tight));
    }

    // ---- edges ----------------------------------------------------------------------------

    @Test
    void exactlyOnEveryLimitStillPasses() {
        // Needs 1300 kg of propellant, 150 W, 150 kg of cargo and $185M: give exactly that.
        ShipBuild snug = ShipBuild.empty()
                .with(TestParts.airframe("plane", 1000, 150, 1300.000001, 50, 100))
                .with(THRUSTER)
                .with(TestParts.power("solar", 50, 150, 20))
                .with(TestParts.cargoBay("bay", 50, 100, 2, 1, 5))
                .with(SATELLITE);
        Mission exact = TestParts.mission("exact", 185, DELTA_V);
        assertEquals(List.of(), issues(snug, exact));
    }

    @Test
    void anUnfinishedBuildOnlyAsksForTheMissingParts() {
        // No thruster yet: fuel cannot be worked out, so there is no fuel warning, only "choose a thruster".
        ShipBuild build = ShipBuild.empty().with(AIRFRAME).with(POWER).with(BAY).with(SATELLITE);
        BuildReport report = BuildCalculator.calculate(build, MISSION);
        assertEquals(List.of(new BuildIssue(BuildIssue.Kind.SLOT_EMPTY, ModuleSlot.THRUSTERS, 0, 0)), report.issues());
        assertEquals(1200, report.dryMassKg(), TINY);
        assertEquals(0, report.propellantKg());
        assertEquals(1200, report.totalMassKg(), TINY);
    }

    @Test
    void noPowerSystemYetIsNotAlsoAPowerShortage() {
        ShipBuild build = ShipBuild.empty().with(AIRFRAME).with(THRUSTER).with(BAY).with(SATELLITE);
        assertEquals(List.of(new BuildIssue(BuildIssue.Kind.SLOT_EMPTY, ModuleSlot.POWER, 0, 0)),
                issues(build, MISSION));
    }

    @Test
    void aTripWithNoSpeedChangeNeedsNoFuel() {
        BuildReport report = BuildCalculator.calculate(goodBuild(), TestParts.mission("parked", 200, 0));
        assertEquals(0, report.propellantKg());
        assertTrue(report.canConfirm());
    }

    @Test
    void weightlessPlaceholderPartsDoNotBreakTheFuelSum() {
        // Nothing to push means nothing to burn; the calculator must not divide by a zero dry mass.
        ShipBuild ghost = ShipBuild.empty()
                .with(TestParts.airframe("ghost", 0, 0, 0, 0, 0))
                .with(TestParts.thruster("ghost-engine", 0, ISP, 0, 0));
        BuildReport report = BuildCalculator.calculate(ghost, MISSION);
        assertEquals(0, report.propellantKg());
        assertEquals(0, report.deltaVAvailableMs());
    }

    @Test
    void severalProblemsAreAllReported() {
        ShipBuild build = goodBuild()
                .with(TestParts.satellite("huge", 100, 4, 2.5, 100, 50))
                .with(TestParts.power("dim", 50, 100, 20));
        List<BuildIssue.Kind> kinds = issues(build, MISSION).stream().map(BuildIssue::kind).toList();
        assertEquals(List.of(BuildIssue.Kind.PAYLOAD_TOO_LONG, BuildIssue.Kind.PAYLOAD_TOO_WIDE,
                BuildIssue.Kind.POWER_SHORT), kinds);
    }

    // ---- the shared sample catalog ---------------------------------------------------------

    @Test
    void theSampleStarterBuildPassesEveryCheck() {
        BuildReport report = BuildCalculator.calculate(SampleData.starterBuild(), SampleData.mission());
        assertEquals(List.of(), report.issues());
        assertEquals(2350, report.dryMassKg(), TINY);
        assertEquals(235, report.costMusd(), TINY);
        assertEquals(2350 * (Math.exp(1000 / (300 * 9.80665)) - 1), report.propellantKg(), TINY);
    }

    @Test
    void theSampleIonThrusterOutrunsBodySolar() {
        ShipBuild build = SampleData.starterBuild()
                .with(SampleData.part("sample-ion-thruster"))
                .with(SampleData.part("sample-body-solar"));
        assertTrue(issues(build, SampleData.mission()).stream()
                .anyMatch(issue -> issue.kind() == BuildIssue.Kind.POWER_SHORT));
    }
}
