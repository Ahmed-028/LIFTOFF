package com.liftoff.engine.sim;

import com.liftoff.TestParts;
import com.liftoff.engine.sim.LaunchIssue.Kind;
import com.liftoff.model.LaunchClass;
import com.liftoff.model.MissionDesign;
import com.liftoff.session.stub.BuildReport;
import com.liftoff.session.stub.Mission;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LaunchCheckerTest {

    private static List<LaunchIssue> check(MissionDesign design) {
        return LaunchChecker.check(design, SimFixtures.EARTH, design.launchVehicle(), design.orbit());
    }

    private static List<Kind> kinds(List<LaunchIssue> issues) {
        return issues.stream().map(LaunchIssue::kind).toList();
    }

    private static BuildReport build(double totalMassKg, double costMusd, double deltaVAvailable,
                                     double deltaVRequired) {
        return new BuildReport(totalMassKg, 0, totalMassKg, costMusd, 0, 0, deltaVAvailable, deltaVRequired,
                List.of());
    }

    @Test
    void aDesignThatFitsEverywhereHasNoIssues() {
        assertEquals(List.of(), check(SimFixtures.design()));
    }

    @Test
    void massOverTheRocketsCapacityIsReportedWithBothFigures() {
        MissionDesign design = SimFixtures.design()
                .withLaunchVehicle(TestParts.rocket("small", Map.of(LaunchClass.POLAR, 100.0), 20));

        List<LaunchIssue> issues = check(design);

        assertEquals(List.of(Kind.MASS_OVER_CAPACITY), kinds(issues));
        assertEquals(150, issues.get(0).needed(), 1e-9);
        assertEquals(100, issues.get(0).available(), 1e-9);
        assertEquals("launch.issue.massOverCapacity", issues.get(0).messageKey());
    }

    @Test
    void aRocketWithNoFigureForTheClassOfOrbitIsNotOfferedRatherThanGuessed() {
        MissionDesign design = SimFixtures.design()
                .withLaunchVehicle(TestParts.rocket("leo-only", Map.of(LaunchClass.LOW_INCLINATION, 5000.0), 20));

        assertEquals(List.of(Kind.ROCKET_NOT_OFFERED), kinds(check(design)));
    }

    @Test
    void noRocketCapacityIsKnownYetForTheMoonOrMars() {
        MissionDesign design = SimFixtures.design();
        Mission moon = new Mission("moon", Mission.Destination.MOON, Mission.StepUnit.DAY, 10, 0, 0, 0);

        List<Kind> kinds = kinds(LaunchChecker.check(design, moon, design.launchVehicle(), design.orbit()));

        assertTrue(kinds.contains(Kind.ROCKET_NOT_OFFERED));
    }

    @Test
    void tooLittleDeltaVIsAFuelShortage() {
        MissionDesign design = SimFixtures.design();

        List<LaunchIssue> issues = LaunchChecker.check(design, build(10, 1, 100, 200), SimFixtures.EARTH,
                design.launchVehicle(), design.orbit());

        assertEquals(List.of(Kind.FUEL_SHORT), kinds(issues));
        assertEquals(200, issues.get(0).needed(), 1e-9);
        assertEquals(100, issues.get(0).available(), 1e-9);
    }

    @Test
    void theShipsMassAndCostCountTowardsTheLimits() {
        MissionDesign design = SimFixtures.design();

        List<LaunchIssue> heavy = LaunchChecker.check(design, build(900, 1, 0, 0), SimFixtures.EARTH,
                design.launchVehicle(), design.orbit());
        List<LaunchIssue> costly = LaunchChecker.check(design, build(10, 2000, 0, 0), SimFixtures.EARTH,
                design.launchVehicle(), design.orbit());

        assertEquals(List.of(Kind.MASS_OVER_CAPACITY), kinds(heavy));
        assertEquals(1050, heavy.get(0).needed(), 1e-9);
        assertEquals(List.of(Kind.OVER_BUDGET), kinds(costly));
    }

    @Test
    void anOrbitThatNeverFliesOverTheTargetIsReported() {
        MissionDesign design = SimFixtures.design()
                .withOrbit(TestParts.orbit("tilted", 700, 35, LaunchClass.POLAR));

        List<LaunchIssue> issues = check(design);

        assertEquals(List.of(Kind.ORBIT_CANNOT_REACH_TARGET), kinds(issues));
        assertEquals(80, issues.get(0).needed(), 1e-9);
        assertEquals(35, issues.get(0).available(), 1e-9);
    }

    @Test
    void theTargetLatitudeIsOnlyCheckedForEarthMissions() {
        MissionDesign design = SimFixtures.design()
                .withOrbit(TestParts.orbit("tilted", 700, 35, LaunchClass.POLAR));
        Mission mars = new Mission("mars", Mission.Destination.MARS, Mission.StepUnit.MONTH, 12, 0, 0, 0);

        List<Kind> kinds = kinds(LaunchChecker.check(design, mars, design.launchVehicle(), design.orbit()));

        assertFalse(kinds.contains(Kind.ORBIT_CANNOT_REACH_TARGET));
    }

    @Test
    void spendingOverTheCapIsReported() {
        List<LaunchIssue> issues = check(SimFixtures.design().withObjective(SimFixtures.objective(80, 40, 200)));

        assertEquals(List.of(Kind.OVER_BUDGET), kinds(issues));
        assertEquals(47, issues.get(0).needed(), 1e-9);
        assertEquals(40, issues.get(0).available(), 1e-9);
    }

    @Test
    void finishingAfterTheDeadlineIsReported() {
        List<LaunchIssue> issues = check(SimFixtures.design().withObjective(SimFixtures.objective(80, 1000, 30)));

        assertEquals(List.of(Kind.OVER_DEADLINE), kinds(issues));
        assertEquals(32, issues.get(0).needed(), 1e-9);
        assertEquals(30, issues.get(0).available(), 1e-9);
    }

    @Test
    void everyFailingReasonIsReturnedNotJustTheFirst() {
        MissionDesign design = SimFixtures.design()
                .withObjective(SimFixtures.objective(80, 40, 30))
                .withOrbit(TestParts.orbit("tilted", 700, 35, LaunchClass.POLAR))
                .withLaunchVehicle(TestParts.rocket("small", Map.of(LaunchClass.POLAR, 100.0), 20));

        List<LaunchIssue> issues = LaunchChecker.check(design, build(10, 1, 100, 200), SimFixtures.EARTH,
                design.launchVehicle(), design.orbit());

        assertEquals(List.of(Kind.MASS_OVER_CAPACITY, Kind.FUEL_SHORT, Kind.ORBIT_CANNOT_REACH_TARGET,
                Kind.OVER_BUDGET, Kind.OVER_DEADLINE), kinds(issues));
    }
}
