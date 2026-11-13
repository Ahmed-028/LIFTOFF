package com.liftoff.engine.sim;

import com.liftoff.TestParts;
import com.liftoff.data.CatalogLoader;
import com.liftoff.engine.OrbitMath;
import com.liftoff.model.Catalog;
import com.liftoff.model.Destination;
import com.liftoff.model.Difficulty;
import com.liftoff.model.Mission;
import com.liftoff.model.MissionDesign;
import com.liftoff.model.StepUnit;
import com.liftoff.session.GameSession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MissionSimulatorTest {

    private static final double DAYS = SimRules.DAYS_PER_MONTH;

    private final EventDeck deck = SimFixtures.deck();

    private static SimulationResult quiet(MissionDesign design) {
        return MissionSimulator.run(design, SimFixtures.EARTH, Difficulty.EASY, 1, EventDeck.empty(),
                SimExtras.none());
    }

    private SimulationResult eventful(MissionDesign design, long seed) {
        return MissionSimulator.run(design, SimFixtures.EARTH, Difficulty.HARD, seed, deck,
                SimExtras.none());
    }

    /** The fixture design's contact-limited downlink in gigabits per day, worked out from OrbitMath. */
    private static double downlinkGbitPerDay(MissionDesign design) {
        double contactMinutes = OrbitMath.contactMinutesPerDay(design.orbit(), design.stations().get(0).latitudeDeg());
        return design.bus().downlinkMbps() * contactMinutes * 60 / 1000;
    }

    @Test
    void missionOneIsPlayedFromDesignToResultWithNoUserInterface() {
        Catalog catalog = CatalogLoader.loadFromClasspath();
        GameSession session = new GameSession(catalog);
        session.selectMission("farm-watch");
        session.chooseBus("eo1-class");
        session.addInstrument("ali");
        session.chooseOrbit("sso-705");
        session.toggleStation("svalbard");
        session.chooseLaunchVehicle("atlas-v-401");
        MissionDesign design = session.design();
        Mission mission = SimFixtures.mission("farm-watch", 0);

        assertEquals(List.of(), LaunchChecker.check(design, mission, design.launchVehicle(), design.orbit()));
        SimulationResult result = MissionSimulator.run(design, mission, Difficulty.EASY, 2026);

        // Independent of the simulator: kbps x 86,400 / 1e6 produced, Mbps x contact minutes x 60 / 1000 sent.
        double producedPerDay = catalog.instrument("ali").orElseThrow().avgDataRateKbps() * 86_400 / 1e6;
        double contactMinutes = OrbitMath.contactMinutesPerDay(catalog.orbit("sso-705").orElseThrow(),
                catalog.groundStation("svalbard").orElseThrow().latitudeDeg());
        double downlinkPerDay = catalog.bus("eo1-class").orElseThrow().downlinkMbps() * contactMinutes * 60 / 1000;
        double expectedSent = Math.min(producedPerDay, downlinkPerDay) * DAYS * design.scienceMonths();

        assertEquals(design.scienceMonths(), result.timeline().size());
        assertEquals(expectedSent, result.totalSentGbit(), expectedSent * 1e-9);
        assertEquals(0, result.totalLostGbit(), 1e-9);
        assertTrue(result.events().isEmpty());
        assertEquals(SimLimit.NONE, result.firstLimit());
        assertTrue(result.success());
    }

    @Test
    void whenTheRadioIsTheBottleneckTheDataSentIsTheContactLimitedValue() {
        MissionDesign design = SimFixtures.design()
                .withBus(TestParts.bus("slow-radio", 100, 100, 0.01, 10, 12));
        double expectedSent = downlinkGbitPerDay(design) * DAYS * 12;

        SimulationResult result = quiet(design);

        assertTrue(expectedSent < result.totalProducedGbit());
        assertEquals(expectedSent, result.totalSentGbit(), expectedSent * 1e-9);
        assertEquals(SimLimit.DOWNLINK, result.firstLimit());
        assertEquals(0, result.firstLimitIndex());
    }

    @Test
    void theSameSeedGivesTheSameResult() {
        assertEquals(eventful(SimFixtures.design(), 99), eventful(SimFixtures.design(), 99));
    }

    @Test
    void differentSeedsCanGiveDifferentResultsWhenEventsAreOn() {
        SimulationResult first = eventful(SimFixtures.design(), 0);
        boolean anyDiffers = false;
        for (long seed = 1; seed <= 20; seed++) {
            anyDiffers |= !first.equals(eventful(SimFixtures.design(), seed));
        }

        assertTrue(anyDiffers);
    }

    @Test
    void cadetHasNoEventsEvenWithAFullDeck() {
        SimulationResult result = MissionSimulator.run(SimFixtures.design(), SimFixtures.EARTH, Difficulty.EASY,
                5, deck, SimExtras.none());

        assertTrue(result.events().isEmpty());
        assertEquals(quiet(SimFixtures.design()), result);
    }

    @Test
    void dataSentNeverExceedsDataProducedAndNothingIsNegative() {
        MissionDesign tight = SimFixtures.design().withBus(TestParts.bus("slow-radio", 100, 40, 0.01, 10, 12));
        for (long seed = 0; seed < 50; seed++) {
            SimulationResult result = MissionSimulator.run(tight, SimFixtures.EARTH, Difficulty.HARD,
                    seed, deck, new SimExtras(5, 0, true));
            double produced = 0;
            double sent = 0;
            for (SimStep step : result.timeline()) {
                produced += step.dataProducedGbit();
                sent += step.dataSentGbit();
                assertTrue(sent <= produced + 1e-9, "seed " + seed + " step " + step.index());
                assertTrue(step.dataProducedGbit() >= 0 && step.dataSentGbit() >= 0 && step.dataLostGbit() >= 0
                        && step.storageUsedGbit() >= 0 && step.powerShortfallW() >= 0 && step.suppliesLeftKg() >= 0);
            }
            assertEquals(produced, result.totalProducedGbit(), 1e-9);
            assertEquals(sent, result.totalSentGbit(), 1e-9);
            assertEquals(result.totalProducedGbit(),
                    result.totalSentGbit() + result.totalLostGbit() + result.storedAtEndGbit(), 1e-6);
        }
    }

    @Test
    void withNoGroundStationsNothingIsSent() {
        SimulationResult result = quiet(SimFixtures.design().minusStation("north"));

        assertTrue(result.totalProducedGbit() > 0);
        assertEquals(0, result.totalSentGbit());
        assertFalse(result.success());
    }

    @Test
    void aPowerShortfallReducesTheDataButDoesNotZeroIt() {
        SimulationResult full = quiet(SimFixtures.design());
        // 25 W supplied against 50 W drawn: the instrument is on half the time.
        SimulationResult starved = quiet(SimFixtures.design().withBus(TestParts.bus("weak", 100, 25, 10, 10, 12)));

        assertEquals(full.totalProducedGbit() / 2, starved.totalProducedGbit(), 1e-9);
        assertTrue(starved.totalSentGbit() > 0);
        assertEquals(25, starved.timeline().get(0).powerShortfallW(), 1e-9);
        assertEquals(SimLimit.POWER, starved.firstLimit());
        assertEquals(0, starved.firstLimitIndex());
        assertTrue(starved.success());
    }

    @Test
    void dataThatDoesNotFitInStorageIsCountedAsLost() {
        MissionDesign design = SimFixtures.design().minusStation("north");

        SimulationResult result = MissionSimulator.run(design, SimFixtures.EARTH, Difficulty.EASY, 1,
                EventDeck.empty(), new SimExtras(10, 0, true));

        assertEquals(10, result.storedAtEndGbit(), 1e-9);
        assertEquals(result.totalProducedGbit() - 10, result.totalLostGbit(), 1e-9);
        assertTrue(result.timeline().stream().allMatch(step -> step.storageUsedGbit() <= 10 + 1e-9));
    }

    @Test
    void aMoonMissionIsSteppedInDaysAndFailsOnTheDayTheSuppliesRunOut() {
        // 2 people x 5 kg a day = 10 kg a day; 60 kg lasts days 0 to 5.
        Mission moon = SimFixtures.mission("moon", Destination.MOON_SURFACE, StepUnit.DAY, 10, 0, 2, 5);

        SimulationResult shortOf = MissionSimulator.run(SimFixtures.design(), moon, Difficulty.EASY, 1,
                EventDeck.empty(), new SimExtras(Double.POSITIVE_INFINITY, 60, true));
        SimulationResult enough = MissionSimulator.run(SimFixtures.design(), moon, Difficulty.EASY, 1,
                EventDeck.empty(), new SimExtras(Double.POSITIVE_INFINITY, 100, true));

        assertFalse(shortOf.success());
        assertEquals(SimLimit.SUPPLIES, shortOf.firstLimit());
        assertEquals(6, shortOf.firstLimitIndex());
        assertEquals(7, shortOf.timeline().size());
        assertEquals(50, shortOf.timeline().get(0).suppliesLeftKg(), 1e-9);

        assertEquals(10, enough.timeline().size());
        assertEquals(0, enough.timeline().get(9).suppliesLeftKg(), 1e-9);
        assertEquals(SimLimit.NONE, enough.firstLimit());
        assertTrue(enough.success());
    }

    @Test
    void marsDustLowersSolarPowerButNotANuclearSource() {
        Mission mars = SimFixtures.mission("mars", Destination.MARS_SURFACE, StepUnit.MONTH, 24, 0, 0, 0);
        EventDeck dustOnly = new EventDeck(deck.cards().stream()
                .filter(card -> card.type() == SimEventType.DUST_STORM).toList());
        // 50 W supplied, 50 W drawn: any loss of supply shows up as lost data.
        MissionDesign design = SimFixtures.design().withBus(TestParts.bus("exact", 100, 50, 10, 10, 12));
        SimulationResult calm = MissionSimulator.run(design, mars, Difficulty.EASY, 3, dustOnly, SimExtras.none());

        SimulationResult solar = MissionSimulator.run(design, mars, Difficulty.HARD, 3, dustOnly,
                new SimExtras(Double.POSITIVE_INFINITY, 0, true));
        SimulationResult nuclear = MissionSimulator.run(design, mars, Difficulty.HARD, 3, dustOnly,
                new SimExtras(Double.POSITIVE_INFINITY, 0, false));

        assertFalse(solar.events().isEmpty());
        assertTrue(solar.totalProducedGbit() < calm.totalProducedGbit());
        assertTrue(solar.totalProducedGbit() > 0);
        assertEquals(SimLimit.POWER, solar.firstLimit());
        assertEquals(calm.totalProducedGbit(), nuclear.totalProducedGbit(), 1e-9);
        assertEquals(SimLimit.NONE, nuclear.firstLimit());
    }

    @Test
    void aSolarStormStopsScienceForTheSafeModeDays() {
        EventDeck stormOnly = new EventDeck(deck.cards().stream()
                .filter(card -> card.type() == SimEventType.SOLAR_STORM).toList());
        MissionDesign longMission = SimFixtures.design().withScienceMonths(60);
        SimulationResult calm = quiet(longMission);

        SimulationResult stormy = MissionSimulator.run(longMission, SimFixtures.EARTH,
                Difficulty.HARD, 3, stormOnly, SimExtras.none());

        assertFalse(stormy.events().isEmpty());
        assertTrue(stormy.totalProducedGbit() < calm.totalProducedGbit());
        SimEvent first = stormy.events().get(0);
        assertEquals(SimEventType.SOLAR_STORM, first.type());
        assertEquals("event.solarStorm", first.messageKey());
        assertEquals(SimRules.SAFE_MODE_DAYS, first.effect().days(), 1e-9);
    }
}
