package com.liftoff.engine.score;

import com.liftoff.TestParts;
import com.liftoff.engine.ResourceCalculator;
import com.liftoff.engine.sim.Difficulty;
import com.liftoff.engine.sim.EventDeck;
import com.liftoff.engine.sim.LaunchIssue;
import com.liftoff.engine.sim.LaunchIssue.Kind;
import com.liftoff.engine.sim.MissionSimulator;
import com.liftoff.engine.sim.SimExtras;
import com.liftoff.engine.sim.SimFixtures;
import com.liftoff.engine.sim.SimulationResult;
import com.liftoff.model.MeasurementType;
import com.liftoff.model.MissionDesign;
import com.liftoff.session.stub.Mission;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportBuilderTest {

    private static SimulationResult fly(MissionDesign design, Mission mission, SimExtras extras) {
        return MissionSimulator.run(design, mission, Difficulty.CADET, 1, EventDeck.empty(), extras);
    }

    private static Report flown(MissionDesign design) {
        return flown(design, SimFixtures.EARTH, SimExtras.none());
    }

    private static Report flown(MissionDesign design, Mission mission, SimExtras extras) {
        return ReportBuilder.build(design, ResourceCalculator.calculate(design), List.of(),
                fly(design, mission, extras), mission);
    }

    private static Report grounded(MissionDesign design, List<LaunchIssue> issues) {
        return ReportBuilder.build(design, ResourceCalculator.calculate(design), issues, null, SimFixtures.EARTH);
    }

    private static LaunchIssue issue(Kind kind) {
        return new LaunchIssue(kind, 2, 1, kind.messageKey());
    }

    private static List<String> keys(List<WhyCard> cards) {
        return cards.stream().map(WhyCard::messageKey).toList();
    }

    @Test
    void aSuccessfulMissionSaysWhatWorkedAndWhichMarginWasThinnest() {
        Report report = flown(SimFixtures.design());

        assertEquals("report.broke.nothing", report.brokeFirst().messageKey());
        assertEquals("report.try.thinnestMargin", report.tryNext().messageKey());
        // 50 W of 100 W is the fullest gauge of the fixture design.
        assertEquals(List.of("POWER", 50.0, 100.0, "W"), report.tryNext().params());
        assertTrue(keys(report.worked()).containsAll(List.of("report.worked.launch",
                "report.worked.dataReturned", "report.worked.noDataLost", "report.worked.powerEnough")));
    }

    @Test
    void meetingTheRequiredMeasurementsIsListedAsWorking() {
        MissionDesign design = ScoreCalculatorTest.needing(MeasurementType.LAND_IMAGING, 30.0)
                .minusInstrument("camera")
                .plusInstrument(ScoreCalculatorTest.camera(MeasurementType.LAND_IMAGING, 30.0));

        assertTrue(keys(flown(design).worked()).contains("report.worked.measurements"));
    }

    @Test
    void anInstrumentTooCoarseForTheGoalIsNamedBeforeAnythingThatWentWrongLater() {
        MissionDesign design = ScoreCalculatorTest.needing(MeasurementType.LAND_IMAGING, 30.0)
                .minusInstrument("camera")
                .plusInstrument(ScoreCalculatorTest.camera(MeasurementType.LAND_IMAGING, 250.0))
                .minusStation("north");

        Report report = flown(design);

        assertEquals("report.broke.requirementTooCoarse", report.brokeFirst().messageKey());
        assertEquals(List.of("LAND_IMAGING", 30.0, 250.0), report.brokeFirst().params());
        assertEquals("report.try.sharperInstrument", report.tryNext().messageKey());
        assertFalse(keys(report.worked()).contains("report.worked.measurements"));
    }

    @Test
    void aMeasurementNoInstrumentMakesIsReportedAsMissing() {
        Report report = flown(ScoreCalculatorTest.needing(MeasurementType.RAIN_MICROWAVE, null));

        assertEquals("report.broke.requirementMissing", report.brokeFirst().messageKey());
        assertEquals(List.of("RAIN_MICROWAVE"), report.brokeFirst().params());
        assertEquals("report.try.addInstrumentFor", report.tryNext().messageKey());
    }

    @Test
    void theFirstLaunchIssueInTheFixedOrderIsNamedWhateverOrderTheyArriveIn() {
        List<LaunchIssue> issues = new ArrayList<>();
        for (Kind kind : Kind.values()) {
            issues.add(0, issue(kind));
        }

        Report report = grounded(SimFixtures.design(), issues);

        assertEquals("report.broke.massOverCapacity", report.brokeFirst().messageKey());
        assertEquals(List.of(2.0, 1.0), report.brokeFirst().params());
        assertFalse(keys(report.worked()).contains("report.worked.launch"));
    }

    @Test
    void eachLaunchIssueGetsTheSuggestionThatFixesIt() {
        Map<Kind, String> expected = Map.of(
                Kind.MASS_OVER_CAPACITY, "report.try.biggerRocketOrLighterShip",
                Kind.ROCKET_NOT_OFFERED, "report.try.otherRocket",
                Kind.FUEL_SHORT, "report.try.moreFuel",
                Kind.ORBIT_CANNOT_REACH_TARGET, "report.try.steeperOrbit",
                Kind.OVER_BUDGET, "report.try.cheaperParts",
                Kind.OVER_DEADLINE, "report.try.shorterScienceOrFasterParts");
        for (Kind kind : Kind.values()) {
            Report report = grounded(SimFixtures.design(), List.of(issue(kind)));

            assertEquals(expected.get(kind), report.tryNext().messageKey(), kind.name());
            assertTrue(report.brokeFirst().messageKey().startsWith("report.broke."), kind.name());
        }
    }

    @Test
    void aPowerShortfallIsNamedWithItsSizeAndTheSuggestionIsMorePower() {
        // 25 W supplied against 50 W drawn.
        Report report = flown(SimFixtures.design().withBus(TestParts.bus("weak", 100, 25, 10, 10, 12)));

        assertEquals("report.broke.power", report.brokeFirst().messageKey());
        assertEquals(List.of(1, "MONTH", 25.0), report.brokeFirst().params());
        assertEquals("report.try.morePowerOrFewerInstruments", report.tryNext().messageKey());
        assertFalse(keys(report.worked()).contains("report.worked.powerEnough"));
    }

    @Test
    void withNoStationsTheSuggestionIsToAddOne() {
        Report report = flown(SimFixtures.design().minusStation("north"));

        assertEquals("report.broke.downlink", report.brokeFirst().messageKey());
        assertEquals("report.try.addStation", report.tryNext().messageKey());
        assertFalse(keys(report.worked()).contains("report.worked.dataReturned"));
    }

    @Test
    void withStationsButASlowRadioTheSuggestionIsMoreContactOrAFasterRadio() {
        Report report = flown(SimFixtures.design().withBus(TestParts.bus("slow-radio", 100, 100, 0.01, 10, 12)));

        assertEquals("report.broke.downlink", report.brokeFirst().messageKey());
        assertEquals("report.try.addStationOrFasterRadio", report.tryNext().messageKey());
    }

    @Test
    void whenDataIsLostFromAFullRecorderTheSuggestionIsToAddAStation() {
        Report report = flown(SimFixtures.design().minusStation("north"), SimFixtures.EARTH,
                new SimExtras(10, 0, true));

        assertEquals("report.broke.storage", report.brokeFirst().messageKey());
        assertEquals("report.try.addStation", report.tryNext().messageKey());
        assertFalse(keys(report.worked()).contains("report.worked.noDataLost"));
    }

    @Test
    void runningOutOfSuppliesNamesTheDayAndSuggestsMore() {
        // 2 people x 5 kg a day with 60 kg on board: day 7 is the first without supplies.
        Mission moon = new Mission("moon", Mission.Destination.MOON, Mission.StepUnit.DAY, 10, 0, 2, 5);

        Report report = flown(SimFixtures.design(), moon, new SimExtras(Double.POSITIVE_INFINITY, 60, true));

        assertEquals("report.broke.supplies", report.brokeFirst().messageKey());
        assertEquals(List.of(7, "DAY"), report.brokeFirst().params());
        assertEquals("report.try.moreSuppliesOrShorterStay", report.tryNext().messageKey());
    }

    @Test
    void aFlightWithNothingToMeasureSaysSo() {
        Report report = flown(SimFixtures.design().minusInstrument("camera"));

        assertEquals("report.broke.noDataReturned", report.brokeFirst().messageKey());
        assertEquals("report.try.addInstrument", report.tryNext().messageKey());
    }

    @Test
    void everyCardHasAFallbackTextThatRendersItsParameters() {
        List<Report> reports = List.of(
                flown(SimFixtures.design()),
                flown(SimFixtures.design().minusStation("north")),
                flown(SimFixtures.design().withBus(TestParts.bus("weak", 100, 25, 10, 10, 12))),
                flown(ScoreCalculatorTest.needing(MeasurementType.RAIN_MICROWAVE, null)),
                grounded(SimFixtures.design(), List.of(issue(Kind.FUEL_SHORT))));
        for (Report report : reports) {
            List<WhyCard> cards = new ArrayList<>(report.worked());
            cards.add(report.brokeFirst());
            cards.add(report.tryNext());
            for (WhyCard card : cards) {
                assertNotNull(ReportTexts.pattern(card.messageKey()), card.messageKey());
                assertFalse(card.defaultText().isBlank(), card.messageKey());
                assertFalse(card.defaultText().contains("{"), card.defaultText());
            }
        }
        for (String key : ReportBuilder.messageKeys()) {
            assertNotNull(ReportTexts.pattern(key), key);
        }
    }
}
