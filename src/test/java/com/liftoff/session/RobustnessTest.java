package com.liftoff.session;

import com.liftoff.data.CatalogLoader;
import com.liftoff.engine.BuildCalculator;
import com.liftoff.engine.BuildLoad;
import com.liftoff.engine.ResourceType;
import com.liftoff.engine.score.ReportBuilder;
import com.liftoff.model.Catalog;
import com.liftoff.model.Difficulty;
import com.liftoff.model.ModuleSlot;
import com.liftoff.model.ShipBuild;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Null, empty and boundary inputs through the one door the interface uses. Every refusal must be
 * a deliberate IllegalArgumentException or IllegalStateException that leaves the session as it was.
 */
class RobustnessTest {

    private static final List<String> FARM_PARTS =
            List.of("eo1-class", "mr-106", "pumpkin-clamshell", "bay-6u", "cubesat-3u");

    private static Catalog catalog;

    private GameSession session;
    private final AtomicInteger changes = new AtomicInteger();

    @BeforeAll
    static void loadCatalog() {
        catalog = CatalogLoader.loadFromClasspath();
    }

    @BeforeEach
    void newSession() {
        session = new GameSession(catalog);
        changes.set(0);
    }

    /** Farm Watch with a complete ship and flight plan, standing on Review. */
    private void designFarmWatch() {
        session.selectMission("farm-watch");
        session.next();
        FARM_PARTS.forEach(session::choosePart);
        session.next();
        session.addInstrument("ali");
        session.chooseOrbit("sso-705");
        session.toggleStation("svalbard");
        session.chooseLaunchVehicle("atlas-v-401");
        session.next();
    }

    private void assertRefused(Class<? extends RuntimeException> type, Executable action) {
        var design = session.design();
        var build = session.build();
        var step = session.step();
        int before = changes.get();
        assertThrows(type, action);
        assertEquals(design, session.design());
        assertEquals(build, session.build());
        assertEquals(step, session.step());
        assertEquals(before, changes.get(), "a refused change must not notify listeners");
    }

    // ---- null and unknown inputs --------------------------------------------------------------

    @Test
    void nullIdsAreRefusedLikeUnknownOnes() {
        session.addListener(changes::incrementAndGet);
        assertRefused(IllegalArgumentException.class, () -> session.selectMission(null));
        session.selectMission("farm-watch");
        session.next();
        assertRefused(IllegalArgumentException.class, () -> session.choosePart(null));
        assertRefused(IllegalArgumentException.class, () -> session.addInstrument(null));
        assertRefused(IllegalArgumentException.class, () -> session.chooseOrbit(null));
        assertRefused(IllegalArgumentException.class, () -> session.toggleStation(null));
        assertRefused(IllegalArgumentException.class, () -> session.chooseLaunchVehicle(null));
        assertRefused(IllegalArgumentException.class, () -> session.chooseBus(null));
        assertRefused(IllegalArgumentException.class, () -> session.goTo(null));
        assertRefused(IllegalArgumentException.class, () -> session.setDifficulty(null));
        assertRefused(IllegalArgumentException.class, () -> session.addListener(null));
    }

    @Test
    void unknownIdsAreRefusedEverywhere() {
        session.addListener(changes::incrementAndGet);
        session.selectMission("farm-watch");
        session.next();
        for (String id : List.of("", " ", "no-such-thing", "FARM-WATCH")) {
            assertRefused(IllegalArgumentException.class, () -> session.selectMission(id));
            assertRefused(IllegalArgumentException.class, () -> session.choosePart(id));
            assertRefused(IllegalArgumentException.class, () -> session.addInstrument(id));
            assertRefused(IllegalArgumentException.class, () -> session.chooseOrbit(id));
            assertRefused(IllegalArgumentException.class, () -> session.toggleStation(id));
            assertRefused(IllegalArgumentException.class, () -> session.chooseLaunchVehicle(id));
        }
        session.removeInstrument("no-such-thing");   // removing what is not there is not an error
        session.removeInstrument(null);
        session.removeListener(null);
    }

    @Test
    void crewSuppliesMustBeARealNonNegativeMass() {
        session.selectMission("moon-landing");
        session.next();
        List.of("apollo-service-module", "rl10c-1", "shuttle-fuel-cell", "apollo-11-adapter", "apollo-11-lander")
                .forEach(session::choosePart);
        session.next();
        for (double bad : new double[] {-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertRefused(IllegalArgumentException.class, () -> session.setCrewSuppliesKg(bad));
        }
        session.setCrewSuppliesKg(0);
        assertEquals(0, session.crewSuppliesKg());
    }

    // ---- nothing chosen -----------------------------------------------------------------------

    @Test
    void aSessionWithNoMissionAnswersEveryQuestionAndRefusesEveryFlight() {
        assertEquals(MissionStep.BRIEFING, session.step());
        assertEquals(5, session.buildReport().issues().size());
        assertEquals(0, session.requiredCrewSuppliesKg());
        assertTrue(session.designIssues().isEmpty());
        assertEquals(0, session.missionResources().get(ResourceType.MASS).used());
        assertTrue(session.report().isEmpty());

        assertEquals(GameSession.NO_MISSION,
                assertThrows(IllegalStateException.class, session::runLaunchCheck).getMessage());
        assertEquals(GameSession.NO_MISSION,
                assertThrows(IllegalStateException.class, () -> session.fly(1)).getMessage());
        assertThrows(IllegalStateException.class, session::confirmBuild);
        assertThrows(IllegalStateException.class, session::redesign);
        assertThrows(IllegalStateException.class, () -> session.setCrewSuppliesKg(10));
        session.back();
        assertEquals(MissionStep.BRIEFING, session.step());
    }

    @Test
    void anEmptyShipHasAReportForEveryMission() {
        for (var mission : catalog.missions()) {
            session.selectMission(mission.id());
            var report = session.buildReport();
            assertFalse(report.canConfirm());
            assertEquals(0, report.dryMassKg());
            assertEquals(0, report.propellantKg());
            assertTrue(Double.isFinite(report.deltaVAvailableMs()));
            assertTrue(session.missionResources().usages().values().stream()
                    .allMatch(usage -> Double.isFinite(usage.used())));
        }
    }

    // ---- flow boundaries ----------------------------------------------------------------------

    @Test
    void goingToALockedStepIsRefusedWithItsReasonAndLeavesThePlayerWhereTheyWere() {
        session.addListener(changes::incrementAndGet);
        session.selectMission("farm-watch");
        for (MissionStep locked : List.of(MissionStep.SHIP, MissionStep.SUPPLIES, MissionStep.REVIEW,
                MissionStep.LAUNCH, MissionStep.FLY, MissionStep.REPORT)) {
            String reason = session.reasonLocked(locked).orElseThrow();
            int before = changes.get();
            assertEquals(reason, assertThrows(IllegalStateException.class, () -> session.goTo(locked)).getMessage());
            assertEquals(MissionStep.BRIEFING, session.step());
            assertEquals(before, changes.get());
        }
    }

    @Test
    void confirmingTwiceIsHarmless() {
        designFarmWatch();
        session.confirmBuild();
        session.confirmBuild();

        assertEquals(MissionStep.REVIEW, session.step());
        assertTrue(session.isUnlocked(MissionStep.LAUNCH));
        session.next();
        assertEquals(List.of(), session.runLaunchCheck());
        assertEquals(List.of(), session.runLaunchCheck());
        session.next();
        session.fly(3);
        assertEquals(GameSession.ALREADY_FLOWN,
                assertThrows(IllegalStateException.class, () -> session.fly(3)).getMessage());
        assertEquals(1, session.missionLog().entries().size());
    }

    @Test
    void swappingPartsRapidlyNeverLosesAPartOrDriftsFromAFreshCalculation() {
        session.selectMission("farm-watch");
        session.next();
        FARM_PARTS.forEach(session::choosePart);
        String[] thrusters = {"mr-103", "mr-106", "monarc-1", "monarc-5", "bit-3", "shuttle-oms"};
        String[] power = {"sp1x-solar", "sp6x-solar", "sp6x-wings", "pumpkin-clamshell", "shuttle-fuel-cell"};

        for (int i = 0; i < 3000; i++) {
            session.choosePart(thrusters[i % thrusters.length]);
            session.choosePart(power[(i * 7) % power.length]);
            session.choosePart(i % 2 == 0 ? "cubesat-1u" : "cubesat-3u");
        }

        ShipBuild build = session.build();
        assertTrue(build.isComplete());
        assertEquals("eo1-class", build.part(ModuleSlot.AIRFRAME).orElseThrow().id());
        assertEquals("bay-6u", build.part(ModuleSlot.CARGO_BAY).orElseThrow().id());
        assertEquals("shuttle-oms", build.part(ModuleSlot.THRUSTERS).orElseThrow().id());
        assertEquals("cubesat-3u", build.part(ModuleSlot.SATELLITE).orElseThrow().id());
        assertEquals(BuildCalculator.calculate(build, catalog.mission("farm-watch").orElseThrow().toMission(),
                BuildLoad.NONE), session.buildReport());
        assertEquals(MissionStep.SHIP, session.step());
    }

    @Test
    void changingAPartAfterConfirmingSendsThePlayerBackThroughReview() {
        designFarmWatch();
        session.confirmBuild();
        session.next();
        assertEquals(MissionStep.LAUNCH, session.step());

        session.choosePart("cubesat-1u");

        assertEquals(MissionStep.REVIEW, session.step());
        assertFalse(session.isUnlocked(MissionStep.LAUNCH));
        assertThrows(IllegalStateException.class, session::runLaunchCheck);
    }

    @Test
    void redesignIsOnlyOfferedFromTheReport() {
        designFarmWatch();
        assertEquals(MissionFlow.REDESIGN_ONLY_FROM_REPORT,
                assertThrows(IllegalStateException.class, session::redesign).getMessage());
        assertEquals(MissionStep.REVIEW, session.step());
    }

    // ---- flight boundaries --------------------------------------------------------------------

    @Test
    void aFlightOfZeroMonthsEndsAtOnceWithAnHonestReportAndAFiniteScore() {
        designFarmWatch();
        session.goTo(MissionStep.SUPPLIES);
        session.setScienceMonths(0);
        session.next();
        session.confirmBuild();
        session.next();
        assertEquals(List.of(), session.runLaunchCheck());
        session.next();

        var flight = session.fly(1);

        assertTrue(flight.timeline().isEmpty());
        assertFalse(flight.success());
        assertEquals(ReportBuilder.BROKE_NO_DATA_RETURNED,
                session.report().orElseThrow().brokeFirst().messageKey());
        var score = session.score().orElseThrow();
        assertTrue(Double.isFinite(score.total()) && score.total() >= 0 && score.total() <= 100, score::toString);
        assertEquals(1, score.stars());
    }

    @Test
    void everyDifficultyFliesEveryLadderMissionOnManySeedsWithoutAnError() {
        var plans = List.of(
                new String[][] {{"farm-watch"}, FARM_PARTS.toArray(String[]::new), {"ali"}, {"atlas-v-401"}},
                new String[][] {{"moon-landing"}, {"apollo-service-module", "rl10c-1", "shuttle-fuel-cell",
                        "apollo-11-adapter", "apollo-11-lander"}, {"lola", "diviner", "apollo-psep"}, {"saturn-v"}},
                new String[][] {{"mars-drones"}, {"curiosity-cruise", "mr-106", "mmrtg", "mars-2020-entry",
                        "perseverance"}, {"mastcam-z", "supercam", "rimfax", "meda"}, {"atlas-v-541"}});
        for (String[][] plan : plans) {
            for (Difficulty difficulty : Difficulty.values()) {
                for (long seed = -20; seed <= 20; seed++) {
                    GameSession game = new GameSession(catalog);
                    game.selectMission(plan[0][0]);
                    game.next();
                    List.of(plan[1]).forEach(game::choosePart);
                    game.next();
                    List.of(plan[2]).forEach(game::addInstrument);
                    if (plan[0][0].equals("farm-watch")) {
                        game.chooseOrbit("sso-705");
                    } else if (plan[0][0].equals("moon-landing")) {
                        game.setCrewSuppliesKg(80);
                    }
                    game.toggleStation("svalbard");
                    game.chooseLaunchVehicle(plan[3][0]);
                    game.setDifficulty(difficulty);
                    game.next();
                    game.confirmBuild();
                    game.next();
                    assertEquals(List.of(), game.runLaunchCheck(), plan[0][0]);
                    game.next();
                    var flight = game.fly(seed);
                    assertTrue(flight.totalSentGbit() <= flight.totalProducedGbit() + 1e-9);
                    assertTrue(Double.isFinite(game.score().orElseThrow().total()));
                    assertTrue(game.report().isPresent());
                    flight.events().forEach(event -> assertFalse(event.messageKey().isBlank()));
                }
            }
        }
    }

    // ---- the mission log on disk --------------------------------------------------------------

    @Test
    void aMissingOrBrokenLogFileNeverStopsAMissionFromFinishing(@TempDir Path folder) throws IOException {
        Path missing = folder.resolve("not").resolve("there").resolve("mission-log.json");
        Path corrupt = Files.writeString(folder.resolve("corrupt.json"), "\u0000\u0001 not json [[[");
        Path empty = Files.writeString(folder.resolve("empty.json"), "");
        Path nulls = Files.writeString(folder.resolve("nulls.json"), "[null, 7, \"x\", {}]");
        Path directory = Files.createDirectory(folder.resolve("a-folder.json"));

        for (Path file : List.of(missing, corrupt, empty, nulls, directory)) {
            MissionLog log = new MissionLog(file);
            assertTrue(log.entries().isEmpty(), file.toString());
            session = new GameSession(catalog, catalog::shipPart, log);
            designFarmWatch();
            session.confirmBuild();
            session.next();
            session.runLaunchCheck();
            session.next();
            session.fly(5);
            assertEquals(1, log.entries().size(), file.toString());
        }
        assertEquals(1, new MissionLog(missing).entries().size());
        assertTrue(Files.isDirectory(directory), "an unwritable target is left alone");
    }

    @Test
    void aLogWrittenBeforeTheDifficultyNamesChangedIsStillRead(@TempDir Path folder) throws IOException {
        Path file = Files.writeString(folder.resolve("mission-log.json"), """
                [ {"missionId":"farm-watch","difficulty":"CADET","score":91.5,"stars":3,"finishedAt":"2026-11-01T10:00:00Z"},
                  {"missionId":"moon-landing","difficulty":"ENGINEER","score":60,"stars":2,"finishedAt":"2026-11-02T10:00:00Z"},
                  {"missionId":"mars-drones","difficulty":"MISSION_DIRECTOR","score":40,"stars":1,"finishedAt":"2026-11-03T10:00:00Z"} ]
                """);

        MissionLog log = new MissionLog(file);

        assertEquals(List.of(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD),
                log.entries().stream().map(MissionLog.Entry::difficulty).toList());
        assertEquals(91.5, log.top(1).getFirst().score());
    }

    @Test
    void oneDamagedEntryDoesNotThrowAwayTheRestOfTheLog(@TempDir Path folder) throws IOException {
        Path file = Files.writeString(folder.resolve("mission-log.json"), """
                [ {"missionId":"farm-watch","difficulty":"EASY","score":91.5,"stars":3,"finishedAt":"2026-11-01T10:00:00Z"},
                  {"missionId":"moon-landing","difficulty":"IMPOSSIBLE","score":"high"},
                  {"player":"someone"},
                  {"missionId":"mars-drones","difficulty":"HARD","score":40,"stars":1,"finishedAt":"2026-11-03T10:00:00Z","extra":true} ]
                """);

        MissionLog log = new MissionLog(file);

        assertEquals(List.of("farm-watch", "mars-drones"),
                log.entries().stream().map(MissionLog.Entry::missionId).toList());
    }

    // ---- flying off the FX thread -------------------------------------------------------------

    private void readyToFly() {
        designFarmWatch();
        session.confirmBuild();
        session.next();
        session.runLaunchCheck();
        session.next();
    }

    @Test
    void aFlightComputedOnAnotherThreadLandsExactlyAsADirectFlight() throws Exception {
        readyToFly();
        var prepared = session.prepareFlight(11);
        var outcome = java.util.concurrent.CompletableFuture.supplyAsync(prepared::compute).get();
        var landed = session.land(outcome);

        GameSession direct = new GameSession(catalog);
        session = direct;
        readyToFly();
        assertEquals(direct.fly(11), landed);
        assertEquals(1, direct.missionLog().entries().size());
    }

    @Test
    void preparingAFlightChangesNothingUntilItLands() {
        readyToFly();
        session.addListener(changes::incrementAndGet);
        var prepared = session.prepareFlight(11);
        var outcome = prepared.compute();

        assertTrue(session.simulation().isEmpty());
        assertEquals(0, changes.get());
        assertEquals(MissionStep.FLY, session.step());

        session.land(outcome);
        assertTrue(session.simulation().isPresent());
        assertEquals(1, changes.get());
    }

    @Test
    void aFlightThatLandsAfterTheDesignChangedIsRefusedAndNothingIsRecorded() {
        readyToFly();
        var outcome = session.prepareFlight(11).compute();
        session.goTo(MissionStep.SUPPLIES);
        session.toggleStation("fairbanks");

        assertEquals(GameSession.FLIGHT_OUTDATED,
                assertThrows(IllegalStateException.class, () -> session.land(outcome)).getMessage());
        assertTrue(session.simulation().isEmpty());
        assertTrue(session.missionLog().entries().isEmpty());
    }

    @Test
    void aFlightCannotLandTwice() {
        readyToFly();
        var outcome = session.prepareFlight(11).compute();
        session.land(outcome);
        assertThrows(IllegalStateException.class, () -> session.land(outcome));
        assertEquals(1, session.missionLog().entries().size());
    }

    @Test
    void anAbsurdScienceTimeIsCappedSoTheFlightStaysSmall() {
        var design = com.liftoff.model.MissionDesign.empty()
                .withObjective(catalog.mission("farm-watch").orElseThrow().briefing())
                .plusInstrument(catalog.instrument("ali").orElseThrow())
                .withScienceMonths(Integer.MAX_VALUE);
        long start = System.nanoTime();
        var flight = com.liftoff.engine.sim.MissionSimulator.run(design,
                catalog.mission("farm-watch").orElseThrow().toMission(), Difficulty.HARD, 1);
        assertEquals(com.liftoff.engine.sim.SimRules.MAX_STEPS, flight.timeline().size());
        assertTrue(System.nanoTime() - start < 2_000_000_000L);
    }
}
