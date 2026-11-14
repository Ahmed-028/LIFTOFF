package com.liftoff.engine.score;

import com.liftoff.TestParts;
import com.liftoff.engine.ResourceCalculator;
import com.liftoff.engine.sim.SimFixtures;
import com.liftoff.engine.sim.SimLimit;
import com.liftoff.engine.sim.SimRules;
import com.liftoff.engine.sim.SimulationResult;
import com.liftoff.model.Capability;
import com.liftoff.model.Instrument;
import com.liftoff.model.MeasurementType;
import com.liftoff.model.MissionDesign;
import com.liftoff.model.Objective;
import com.liftoff.model.Requirement;
import com.liftoff.model.ValueBasis;
import com.liftoff.session.stub.BuildReport;
import com.liftoff.session.stub.Mission;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreCalculatorTest {


    private static final double REQUIRED_GBIT = 100 * 86_400 / 1e6 * SimRules.DAYS_PER_MONTH * 12;

    private static final Mission MOON =
            new Mission("moon", Mission.Destination.MOON, Mission.StepUnit.DAY, 10, 0, 0, 0);

    static SimulationResult flight(double sentGbit, int launchSlipMonths) {
        return new SimulationResult(Mission.StepUnit.MONTH, List.of(), List.of(), sentGbit, sentGbit, 0, 0,
                launchSlipMonths, SimLimit.NONE, -1, sentGbit > 0);
    }

    private static Score score(MissionDesign design, SimulationResult flight) {
        return ScoreCalculator.score(design, ResourceCalculator.calculate(design), null, flight, SimFixtures.EARTH);
    }

    private static Score moonScore(BuildReport build) {
        MissionDesign design = SimFixtures.design();
        return ScoreCalculator.score(design, ResourceCalculator.calculate(design), build, null, MOON);
    }

    private static BuildReport ship(double deltaVAvailable, double deltaVRequired) {
        return new BuildReport(0, 0, 0, 0, 0, 0, deltaVAvailable, deltaVRequired, List.of());
    }

    static MissionDesign needing(MeasurementType type, Double maxResolutionM) {
        Objective objective = new Objective("goal", "goal", "", "", "", 80, false,
                List.of(new Requirement(type, maxResolutionM)), List.of(), 1000, 200, 12, TestParts.SOURCE);
        return SimFixtures.design().withObjective(objective);
    }

    static Instrument camera(MeasurementType type, Double resolutionM) {
        return new Instrument("sharp", "sharp", "sharp", "", "", 50, 50, ValueBasis.PUBLISHED, 100,
                ValueBasis.PUBLISHED, List.of(new Capability(type, resolutionM)), 5, ValueBasis.GAME_ESTIMATE, 12,
                ValueBasis.GAME_ESTIMATE, TestParts.SOURCE);
    }

    @Test
    void aFlightThatReturnsEverythingWithHealthyMarginsScoresFullMarks() {
        Score score = score(SimFixtures.design(), flight(REQUIRED_GBIT, 0));

        assertEquals(60, score.science(), 1e-9);
        assertEquals(20, score.margins(), 1e-9);
        assertEquals(20, score.budgetTime(), 1e-9);
        assertEquals(100, score.total(), 1e-9);
        assertEquals(3, score.stars());
    }

    @Test
    void scienceIsTheShareOfTheRequiredDataReturned() {
        assertEquals(30, score(SimFixtures.design(), flight(REQUIRED_GBIT / 2, 0)).science(), 1e-9);
        assertEquals(0, score(SimFixtures.design(), flight(0, 0)).science(), 1e-9);
    }

    @Test
    void scienceIsCappedWhenMoreThanRequiredIsReturned() {
        assertEquals(60, score(SimFixtures.design(), flight(REQUIRED_GBIT * 3, 0)).science(), 1e-9);
    }

    @Test
    void aMissionThatNeverFlewHasNoScience() {
        assertEquals(0, score(SimFixtures.design(), null).science(), 1e-9);
    }

    @Test
    void aFlightShorterThanTheMinimumScienceDurationReturnsLessThanRequired() {
        MissionDesign halfAsLong = SimFixtures.design().withScienceMonths(6);

        assertEquals(30, score(halfAsLong, flight(REQUIRED_GBIT / 2, 0)).science(), 1e-9);
    }

    @Test
    void aDesignThatCannotMakeARequiredMeasurementScoresNoScience() {
        MissionDesign blind = needing(MeasurementType.LAND_IMAGING, 30.0);
        MissionDesign tooCoarse = blind.minusInstrument("camera")
                .plusInstrument(camera(MeasurementType.LAND_IMAGING, 250.0));

        assertEquals(0, score(blind, flight(REQUIRED_GBIT, 0)).science(), 1e-9);
        assertEquals(0, score(tooCoarse, flight(REQUIRED_GBIT, 0)).science(), 1e-9);
        assertEquals(1, ScoreCalculator.unmetRequirements(tooCoarse).size());
    }

    @Test
    void aDesignThatMakesTheRequiredMeasurementScoresItsScience() {
        MissionDesign sharp = needing(MeasurementType.LAND_IMAGING, 30.0).minusInstrument("camera")
                .plusInstrument(camera(MeasurementType.LAND_IMAGING, 30.0));
        MissionDesign anyDetail = needing(MeasurementType.RAIN_MICROWAVE, null).minusInstrument("camera")
                .plusInstrument(camera(MeasurementType.RAIN_MICROWAVE, null));

        assertEquals(60, score(sharp, flight(REQUIRED_GBIT, 0)).science(), 1e-9);
        assertEquals(60, score(anyDetail, flight(REQUIRED_GBIT, 0)).science(), 1e-9);
    }

    @Test
    void aTightResourceEarnsHalfItsMarginCreditAndAnOverOrUnknownOneEarnsNone() {

        MissionDesign tight = SimFixtures.design().withBus(TestParts.bus("bus", 100, 55, 10, 10, 12));
        MissionDesign over = SimFixtures.design().withBus(TestParts.bus("bus", 100, 40, 10, 10, 12));
        MissionDesign unknown = SimFixtures.design().withLaunchVehicle(null);

        assertEquals(20 * 5.5 / 6, score(tight, null).margins(), 1e-9);
        assertEquals(20 * 5.0 / 6, score(over, null).margins(), 1e-9);
        assertEquals(20 * 5.0 / 6, score(unknown, null).margins(), 1e-9);
    }

    @Test
    void fuelIsASeventhMarginBeyondEarthOrbit() {
        assertEquals(20, moonScore(ship(1000, 100)).margins(), 1e-9);
        assertEquals(20 * 6.5 / 7, moonScore(ship(100, 90)).margins(), 1e-9);
        assertEquals(20 * 6.0 / 7, moonScore(ship(100, 200)).margins(), 1e-9);
        assertEquals(20 * 6.0 / 7, moonScore(null).margins(), 1e-9);
    }

    @Test
    void spendingOverTheCapScalesTheBudgetPointsDown() {

        MissionDesign overspent = SimFixtures.design().withObjective(SimFixtures.objective(80, 23.5, 200));

        assertEquals(5 + 10, score(overspent, null).budgetTime(), 1e-9);
    }

    @Test
    void finishingLateScalesTheTimePointsDownAndALaunchSlipCounts() {

        MissionDesign late = SimFixtures.design().withObjective(SimFixtures.objective(80, 1000, 16));
        MissionDesign justInTime = SimFixtures.design().withObjective(SimFixtures.objective(80, 1000, 32));

        assertEquals(10 + 5, score(late, null).budgetTime(), 1e-9);
        assertEquals(20, score(justInTime, flight(REQUIRED_GBIT, 0)).budgetTime(), 1e-9);
        assertEquals(10 + 10 * 32.0 / 34, score(justInTime, flight(REQUIRED_GBIT, 2)).budgetTime(), 1e-9);
    }

    @Test
    void theShipsCostCountsAgainstTheBudget() {
        MissionDesign design = SimFixtures.design().withObjective(SimFixtures.objective(80, 47, 200));
        BuildReport costly = new BuildReport(0, 0, 0, 47, 0, 0, 0, 0, List.of());

        Score score = ScoreCalculator.score(design, ResourceCalculator.calculate(design), costly, null,
                SimFixtures.EARTH);

        assertEquals(5 + 10, score.budgetTime(), 1e-9);
    }

    @Test
    void starsFollowTheNamedThresholds() {
        assertEquals(1, ScoreCalculator.stars(0));
        assertEquals(1, ScoreCalculator.stars(ScoreRules.TWO_STARS_FROM - 0.01));
        assertEquals(2, ScoreCalculator.stars(ScoreRules.TWO_STARS_FROM));
        assertEquals(2, ScoreCalculator.stars(ScoreRules.THREE_STARS_FROM - 0.01));
        assertEquals(3, ScoreCalculator.stars(ScoreRules.THREE_STARS_FROM));
        assertEquals(3, ScoreCalculator.stars(100));
    }

    @Test
    void theWeightsAddUpToOneHundred() {
        assertEquals(100, ScoreRules.SCIENCE_POINTS + ScoreRules.MARGIN_POINTS + ScoreRules.BUDGET_POINTS
                + ScoreRules.TIME_POINTS, 1e-9);
    }

    @Test
    void moreScienceNeverLowersTheScoreAndTheScoreStaysBetweenZeroAndOneHundred() {
        MissionDesign[] designs = {
                SimFixtures.design(),
                SimFixtures.design().withObjective(SimFixtures.objective(80, 1, 1)),
                SimFixtures.design().withBus(null).withLaunchVehicle(null).withOrbit(null)
        };
        for (MissionDesign design : designs) {
            double previous = -1;
            for (int tenth = 0; tenth <= 15; tenth++) {
                Score score = score(design, flight(REQUIRED_GBIT * tenth / 10, 0));
                assertTrue(score.total() >= previous, "dropped at " + tenth);
                assertTrue(score.total() >= 0 && score.total() <= 100);
                assertTrue(score.science() >= 0 && score.science() <= 60);
                assertTrue(score.margins() >= 0 && score.margins() <= 20);
                assertTrue(score.budgetTime() >= 0 && score.budgetTime() <= 20);
                assertTrue(score.stars() >= 1 && score.stars() <= 3);
                assertEquals(score.science() + score.margins() + score.budgetTime(), score.total(), 1e-9);
                previous = score.total();
            }
        }
    }
}
