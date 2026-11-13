package com.liftoff.session;

import com.liftoff.data.CatalogLoader;
import com.liftoff.engine.ResourceLevel;
import com.liftoff.engine.ResourceType;
import com.liftoff.engine.score.Report;
import com.liftoff.engine.score.Score;
import com.liftoff.engine.score.ScoreRules;
import com.liftoff.engine.sim.Difficulty;
import com.liftoff.engine.sim.LaunchIssue;
import com.liftoff.engine.sim.SimLimit;
import com.liftoff.model.Catalog;
import com.liftoff.model.MissionDesign;
import com.liftoff.session.stub.ModuleSlot;
import com.liftoff.session.stub.ShipBuild;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
 * The five ship modules are test fakes (zero mass and cost): the catalog has no ship parts yet.
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
        assertEquals(Difficulty.CADET, saved.get(0).difficulty());
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
    @Disabled("Mission 2 (Moon landing) is not on main: needs missions.json, ship parts, TRANS_LUNAR rocket "
            + "capacity, delta-v and suppliesKgPerCrewDay with sources from Members 1 and 2. No fake data.")
    void missionTwoIsPlayedFromBriefingToReport() {
    }

    @Test
    @Disabled("Mission 3 (Mars sample drones) is not on main: needs missions.json, ship parts, TRANS_MARS rocket "
            + "capacity, delta-v, the solar/nuclear power flag and sourced events from Members 1 and 2. No fake data.")
    void missionThreeIsPlayedFromBriefingToReport() {
    }
}
