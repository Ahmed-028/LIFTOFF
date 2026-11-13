package com.liftoff.session;

import com.liftoff.data.CatalogLoader;
import com.liftoff.engine.BuildIssue;
import com.liftoff.engine.ResourceLevel;
import com.liftoff.engine.ResourceType;
import com.liftoff.engine.score.Report;
import com.liftoff.engine.score.Score;
import com.liftoff.engine.score.ScoreRules;
import com.liftoff.engine.sim.LaunchIssue;
import com.liftoff.engine.sim.SimEventType;
import com.liftoff.engine.sim.SimLimit;
import com.liftoff.model.Catalog;
import com.liftoff.model.Difficulty;
import com.liftoff.model.MissionDesign;
import com.liftoff.model.ModuleSlot;
import com.liftoff.model.ShipBuild;
import com.liftoff.model.StepUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plays Mission 1 from briefing to report through GameSession only, with the real catalog.
 * Zero-mass test modules isolate the launch, flight and score checks in these scenarios.
 */
class FullMissionTest {

    private static final long SEED = 2026;

    private static Catalog catalog;

    @TempDir
    Path folder;

    private MissionLog log;
    private GameSession session;

    @BeforeAll
    static void loadCatalog() {
        catalog = CatalogLoader.loadFromClasspath();
    }

    @BeforeEach
    void newSession() {
        log = new MissionLog(folder.resolve("mission-log.json"));
        session = new GameSession(catalog, FakeParts.lookup(), log);
    }

    /** Briefing, ship and supplies for Farm Watch, up to a confirmed build standing on the launch step. */
    private void designFarmWatch(String busId, String instrumentId, String rocketId) {
        session.selectMission("farm-watch");
        session.next();
        for (ModuleSlot slot : ModuleSlot.values()) {
            session.choosePart(FakeParts.idOf(slot, "a"));
        }
        session.next();
        session.chooseBus(busId);
        session.addInstrument(instrumentId);
        session.chooseOrbit("sso-705");
        session.toggleStation("svalbard");
        session.toggleStation("fairbanks");
        session.chooseLaunchVehicle(rocketId);
        session.next();
        session.confirmBuild();
        session.next();
        assertEquals(MissionStep.LAUNCH, session.step());
    }

    @Test
    void aSoundFarmWatchDesignFliesAndEarnsThreeStars() {
        designFarmWatch("eo1-class", "ali", "atlas-v-401");

        assertEquals(List.of(), session.runLaunchCheck());
        session.next();
        session.fly(SEED);
        session.next();

        assertEquals(MissionStep.REPORT, session.step());
        assertTrue(session.simulation().orElseThrow().success());
        assertEquals(SimLimit.NONE, session.simulation().orElseThrow().firstLimit());

        Score score = session.score().orElseThrow();
        assertEquals(ScoreRules.SCIENCE_POINTS, score.science(), 1e-6);
        assertEquals(ScoreRules.BUDGET_POINTS + ScoreRules.TIME_POINTS, score.budgetTime(), 1e-9);
        assertEquals(3, score.stars());
        assertTrue(score.total() <= 100);

        Report report = session.report().orElseThrow();
        assertEquals("report.broke.nothing", report.brokeFirst().messageKey());
        assertEquals("report.try.thinnestMargin", report.tryNext().messageKey());
        // ALI's 100 W of the EO-1 class bus's 115 W is the tightest gauge of this design.
        assertEquals("POWER", report.tryNext().params().get(0));
        assertEquals(ResourceLevel.TIGHT, session.resources().get(ResourceType.POWER).level());
        assertFalse(report.worked().isEmpty());
    }

    @Test
    void finishingAMissionAddsExactlyOneLogEntry() {
        designFarmWatch("eo1-class", "ali", "atlas-v-401");
        session.runLaunchCheck();
        session.next();
        AtomicInteger calls = new AtomicInteger();
        session.addListener(calls::incrementAndGet);

        session.fly(SEED);
        assertThrows(IllegalStateException.class, () -> session.fly(SEED));
        session.next();

        assertEquals(2, calls.get());
        List<MissionLog.Entry> saved = new MissionLog(folder.resolve("mission-log.json")).entries();
        assertEquals(1, saved.size());
        assertEquals("farm-watch", saved.get(0).missionId());
        assertEquals(Difficulty.EASY, saved.get(0).difficulty());
        assertEquals(session.score().orElseThrow().total(), saved.get(0).score(), 1e-9);
        assertEquals(3, saved.get(0).stars());
        assertFalse(saved.get(0).finishedAt().isBlank());
    }

    @Test
    void theSameSeedReplaysTheSameMission() {
        designFarmWatch("eo1-class", "ali", "atlas-v-401");
        session.runLaunchCheck();
        session.next();
        session.fly(SEED);

        GameSession again = session;
        newSession();
        designFarmWatch("eo1-class", "ali", "atlas-v-401");
        session.runLaunchCheck();
        session.next();
        session.fly(SEED);

        assertEquals(again.simulation(), session.simulation());
        assertEquals(again.score(), session.score());
    }

    @Test
    void modisAloneFliesButIsTooCoarseForFarmWatchAndTheReportSaysSoFirst() {
        designFarmWatch("eo1-class", "modis", "atlas-v-401");

        assertEquals(List.of(), session.runLaunchCheck());
        session.next();
        session.fly(SEED);
        session.next();

        // MODIS also draws more than the bus supplies, but the wrong instrument comes first.
        assertEquals(SimLimit.POWER, session.simulation().orElseThrow().firstLimit());
        Report report = session.report().orElseThrow();
        assertEquals("report.broke.requirementTooCoarse", report.brokeFirst().messageKey());
        assertEquals(List.of("LAND_IMAGING", 30.0, 250.0), report.brokeFirst().params());
        assertEquals("report.try.sharperInstrument", report.tryNext().messageKey());

        Score score = session.score().orElseThrow();
        assertEquals(0, score.science(), 1e-9);
        assertEquals(1, score.stars());
    }

    @Test
    void redesignFromTheReportReturnsToTheShipWithTheDesignIntactAndTheResultsCleared() {
        designFarmWatch("eo1-class", "modis", "atlas-v-401");
        session.runLaunchCheck();
        session.next();
        session.fly(SEED);
        session.next();
        ShipBuild shipBefore = session.build();
        MissionDesign designBefore = session.design();

        session.redesign();

        assertEquals(MissionStep.SHIP, session.step());
        assertEquals(shipBefore, session.build());
        assertEquals(designBefore, session.design());
        assertTrue(session.score().isEmpty());
        assertTrue(session.report().isEmpty());
        assertTrue(session.simulation().isEmpty());
        assertFalse(session.isUnlocked(MissionStep.LAUNCH));

        // Swap the instrument and fly again: this time it works, and a second entry is logged.
        session.removeInstrument("modis");
        session.addInstrument("ali");
        session.goTo(MissionStep.SUPPLIES);
        session.next();
        session.confirmBuild();
        session.next();
        session.runLaunchCheck();
        session.next();
        session.fly(SEED);

        assertEquals("report.broke.nothing", session.report().orElseThrow().brokeFirst().messageKey());
        assertEquals(2, log.entries().size());
        assertEquals("farm-watch", log.top(1).get(0).missionId());
        assertEquals(3, log.top(1).get(0).stars());
    }

    @Test
    void aSpacecraftTooHeavyForItsRocketIsStoppedOnThePadAndNeverFlies() {
        designFarmWatch("landsat7-class", "etm-plus", "electron");

        List<LaunchIssue> issues = session.runLaunchCheck();

        assertEquals(LaunchIssue.Kind.MASS_OVER_CAPACITY, issues.get(0).kind());
        assertEquals(issues, session.launchIssues());
        Report report = session.report().orElseThrow();
        assertEquals("report.broke.massOverCapacity", report.brokeFirst().messageKey());
        assertEquals("report.try.biggerRocketOrLighterShip", report.tryNext().messageKey());
        assertTrue(session.score().isEmpty());
        assertFalse(session.isUnlocked(MissionStep.FLY));
        assertThrows(IllegalStateException.class, () -> session.fly(SEED));
        assertThrows(IllegalStateException.class, session::next);
        assertTrue(log.entries().isEmpty());
    }

    @Test
    void theLaunchCheckCannotRunBeforeTheBuildIsConfirmed() {
        session.selectMission("farm-watch");

        assertThrows(IllegalStateException.class, session::runLaunchCheck);
        assertThrows(IllegalStateException.class, () -> session.fly(SEED));
    }

    @Test
    void missionTwoIsPlayedFromBriefingToReport() {
        session = new GameSession(catalog, catalog::shipPart, log);
        assemble("moon-landing", "apollo-service-module", "rl10c-1", "shuttle-fuel-cell",
                "apollo-11-adapter", "apollo-11-lander");
        session.addInstrument("lola");
        session.chooseLaunchVehicle("saturn-v");
        session.toggleStation("wallops");
        assertTrue(session.buildReport().issues().stream()
                .anyMatch(issue -> issue.kind() == BuildIssue.Kind.SUPPLIES_SHORT));
        session.setCrewSuppliesKg(80);

        finishFlight(SEED);

        var result = session.simulation().orElseThrow();
        assertEquals(StepUnit.DAY, result.stepUnit());
        assertEquals(8, result.timeline().size());
        assertEquals(80 - 8 * 2 * 4.99, result.timeline().getLast().suppliesLeftKg(), 1e-9);
        assertEquals("moon-landing", log.top(1).getFirst().missionId());
    }

    @Test
    void missionThreeIsPlayedFromBriefingToReport() {
        session = new GameSession(catalog, catalog::shipPart, log);
        assemble("mars-drones", "curiosity-cruise", "mr-106", "mmrtg", "mars-2020-entry", "perseverance");
        session.addInstrument("mastcam-z");
        session.addInstrument("supercam");
        session.chooseLaunchVehicle("atlas-v-541");
        session.toggleStation("wallops");
        session.choosePart("sp1x-solar");
        assertTrue(session.buildReport().issues().stream()
                .anyMatch(issue -> issue.kind() == BuildIssue.Kind.POWER_SHORT));
        session.choosePart("mmrtg");

        // This seed draws the bundled InSight dust-storm card during the six-month flight.
        finishFlight(23);

        var result = session.simulation().orElseThrow();
        assertEquals(StepUnit.MONTH, result.stepUnit());
        assertEquals(6, result.timeline().size());
        assertTrue(result.events().stream().anyMatch(event -> event.type() == SimEventType.DUST_STORM));
        assertTrue(result.events().stream().filter(event -> event.type() == SimEventType.DUST_STORM)
                .allMatch(event -> event.effect().fraction() == 0));
        assertTrue(result.timeline().stream().allMatch(step -> step.powerShortfallW() == 0));
        assertEquals("mars-drones", log.top(1).getFirst().missionId());
    }

    private void assemble(String missionId, String... parts) {
        session.selectMission(missionId);
        session.next();
        for (String part : parts) {
            session.choosePart(part);
        }
        session.next();
    }

    private void finishFlight(long seed) {
        session.next();
        assertTrue(session.buildReport().canConfirm(), () -> session.buildReport().issues().toString());
        assertTrue(session.buildReport().propellantKg() > 0);
        assertTrue(session.buildReport().deltaVAvailableMs() >= session.buildReport().deltaVRequiredMs());
        session.confirmBuild();
        session.next();
        assertTrue(session.runLaunchCheck().isEmpty(), () -> session.launchIssues().toString());
        session.next();
        assertTrue(session.fly(seed).success());
        session.next();
        assertEquals(MissionStep.REPORT, session.step());
        assertTrue(session.score().orElseThrow().science() > 0);
        assertTrue(session.report().isPresent());
    }
}
