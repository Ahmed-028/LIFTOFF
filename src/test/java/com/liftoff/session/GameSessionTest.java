package com.liftoff.session;

import com.liftoff.data.CatalogLoader;
import com.liftoff.engine.OrbitMath;
import com.liftoff.engine.ResourceLevel;
import com.liftoff.engine.ResourceReport;
import com.liftoff.engine.ResourceType;
import com.liftoff.model.Catalog;
import com.liftoff.session.stub.ModuleSlot;
import com.liftoff.session.stub.ShipBuild;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSessionTest {

    private static Catalog catalog;
    private GameSession session;

    @BeforeAll
    static void loadCatalog() {
        catalog = CatalogLoader.loadFromClasspath();
    }

    @BeforeEach
    void newSession() {
        session = new GameSession(catalog);
    }

    @Test
    void aNewSessionStartsWithAnEmptyDesign() {
        assertNull(session.design().objective());
        assertTrue(session.design().instruments().isEmpty());
        assertEquals(ResourceLevel.UNKNOWN, session.resources().get(ResourceType.MASS).level());
    }

    @Test
    void selectingAnObjectiveSetsItAndItsMinimumScienceDuration() {
        session.selectObjective("ice-patrol");

        assertEquals("ice-patrol", session.design().objective().id());
        assertEquals(36, session.design().scienceMonths());
    }

    @Test
    void anUnknownIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> session.selectObjective("find-aliens"));
        assertThrows(IllegalArgumentException.class, () -> session.addInstrument("warp-drive"));
    }

    @Test
    void addingAnInstrumentChangesTheResources() {
        session.addInstrument("modis");

        assertEquals(229.0, session.resources().get(ResourceType.MASS).used(), 1e-9);
        assertEquals(162.5, session.resources().get(ResourceType.POWER).used(), 1e-9);
        assertEquals(535.68, session.resources().get(ResourceType.DATA).used(), 1e-9);
    }

    @Test
    void addingTheSameInstrumentTwiceKeepsOne() {
        session.addInstrument("modis");
        session.addInstrument("modis");

        assertEquals(1, session.design().instruments().size());
    }

    @Test
    void removingAnInstrumentGivesItsResourcesBack() {
        session.addInstrument("modis");
        session.removeInstrument("modis");

        assertEquals(0.0, session.resources().get(ResourceType.MASS).used(), 1e-9);
    }

    @Test
    void togglingAStationAddsItThenRemovesIt() {
        session.toggleStation("svalbard");
        assertTrue(session.design().hasStation("svalbard"));

        session.toggleStation("svalbard");
        assertFalse(session.design().hasStation("svalbard"));
    }

    @Test
    void theScienceDurationCanBeChanged() {
        session.selectObjective("farm-watch");
        session.setScienceMonths(48);

        assertEquals(48, session.design().scienceMonths());
    }

    @Test
    void aNegativeScienceDurationIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> session.setScienceMonths(-1));
    }

    @Test
    void listenersAreToldAfterEveryChange() {
        AtomicInteger calls = new AtomicInteger();
        session.addListener(calls::incrementAndGet);

        session.selectObjective("farm-watch");
        session.chooseBus("eo1-class");
        session.addInstrument("ali");

        assertEquals(3, calls.get());
    }

    @Test
    void aRemovedListenerIsNoLongerTold() {
        AtomicInteger calls = new AtomicInteger();
        Runnable listener = calls::incrementAndGet;
        session.addListener(listener);

        session.addInstrument("modis");
        session.removeListener(listener);
        session.addInstrument("ali");

        assertEquals(1, calls.get());
    }

    @Test
    void anIcesatLikeMissionClosesOnAllSixResources() {
        session.selectObjective("ice-patrol");
        session.chooseBus("landsat7-class");
        session.addInstrument("glas");
        session.chooseOrbit("polar-600");
        session.toggleStation("svalbard");
        session.toggleStation("fairbanks");
        session.toggleStation("mcmurdo");
        session.chooseLaunchVehicle("atlas-v-401");

        ResourceReport report = session.resources();

        assertEquals(1857.0, report.get(ResourceType.MASS).used(), 1e-9);
        assertEquals(8080.0, report.get(ResourceType.MASS).capacity().orElseThrow(), 1e-9);
        assertEquals(330.0, report.get(ResourceType.POWER).used(), 1e-9);
        assertEquals(553.55, report.get(ResourceType.BUDGET).used(), 1e-6);
        assertEquals(86.0, report.get(ResourceType.TIME).used(), 1e-9);
        assertFalse(report.anyOver(), report.toString());
    }

    @Test
    void aSmallStormSpacecraftBarelyFitsTheSmallestRocket() {
        session.selectObjective("storm-chasers");
        session.chooseBus("sorce-class");
        session.addInstrument("virs");
        session.chooseOrbit("tropical-402");
        session.chooseLaunchVehicle("electron");


        assertEquals(297.0, session.resources().get(ResourceType.MASS).used(), 1e-9);
        assertEquals(ResourceLevel.TIGHT, session.resources().get(ResourceType.MASS).level());
    }


    private GameSession sessionWithParts() {
        return new GameSession(catalog, FakeParts.lookup());
    }

    private static void chooseAllParts(GameSession game, String suffix) {
        for (ModuleSlot slot : ModuleSlot.values()) {
            game.choosePart(FakeParts.idOf(slot, suffix));
        }
    }

    @Test
    void aNewSessionStartsAtTheBriefingWithAnEmptyBuild() {
        assertEquals(MissionStep.BRIEFING, session.step());
        assertFalse(session.build().isComplete());
        assertFalse(session.buildReport().canConfirm());
    }

    @Test
    void choosingAPartSwapsOnlyItsSlot() {
        GameSession game = sessionWithParts();
        chooseAllParts(game, "a");

        game.choosePart("power-b");

        assertEquals("power-b", game.build().part(ModuleSlot.POWER).orElseThrow().id());
        for (ModuleSlot slot : ModuleSlot.values()) {
            if (slot != ModuleSlot.POWER) {
                assertEquals(FakeParts.idOf(slot, "a"), game.build().part(slot).orElseThrow().id());
            }
        }
    }

    @Test
    void choosingAPartRecalculatesTheBuildReport() {
        GameSession game = sessionWithParts();
        assertEquals(ModuleSlot.values().length, game.buildReport().issues().size());

        game.choosePart("airframe-a");

        assertEquals(ModuleSlot.values().length - 1, game.buildReport().issues().size());
    }

    @Test
    void anUnknownPartIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> sessionWithParts().choosePart("warp-core"));
    }

    @Test
    void confirmBuildThrowsWhileIssuesExist() {
        GameSession game = sessionWithParts();
        game.choosePart("airframe-a");

        assertFalse(game.buildReport().canConfirm());
        assertThrows(IllegalStateException.class, game::confirmBuild);
    }

    @Test
    void aConfirmedBuildOpensLaunchAndChangingAPartLocksItAgain() {
        GameSession game = sessionWithParts();
        game.next();
        game.next();
        chooseAllParts(game, "a");
        game.next();
        game.next();
        game.confirmBuild();
        assertTrue(game.isUnlocked(MissionStep.LAUNCH));

        game.goTo(MissionStep.SHIP);
        game.choosePart("cargo-bay-b");

        assertFalse(game.isUnlocked(MissionStep.LAUNCH));
    }

    @Test
    void goToALockedStepThrowsWithTheReason() {
        IllegalStateException refused =
                assertThrows(IllegalStateException.class, () -> session.goTo(MissionStep.SUPPLIES));

        assertEquals(session.reasonLocked(MissionStep.SUPPLIES).orElseThrow(), refused.getMessage());
        assertEquals(MissionStep.BRIEFING, session.step());
    }

    @Test
    void listenersFireExactlyOncePerBuildOrFlowChange() {
        GameSession game = sessionWithParts();
        AtomicInteger calls = new AtomicInteger();
        game.addListener(calls::incrementAndGet);

        game.selectMission("farm-watch");
        game.next();
        game.next();
        chooseAllParts(game, "a");
        game.goTo(MissionStep.SUPPLIES);
        game.next();
        game.confirmBuild();
        game.back();

        assertEquals(3 + ModuleSlot.values().length + 4, calls.get());
    }

    @Test
    void aRefusedChangeDoesNotTellListeners() {
        AtomicInteger calls = new AtomicInteger();
        session.addListener(calls::incrementAndGet);

        assertThrows(IllegalStateException.class, () -> session.goTo(MissionStep.LAUNCH));
        assertThrows(IllegalStateException.class, session::confirmBuild);
        assertThrows(IllegalArgumentException.class, () -> session.choosePart("warp-core"));
        session.back();

        assertEquals(0, calls.get());
    }

    @Test
    void selectingAMissionResetsTheFlowAndTheDesign() {
        GameSession game = sessionWithParts();
        game.next();
        game.next();
        chooseAllParts(game, "a");
        game.next();
        game.addInstrument("modis");

        game.selectMission("ice-patrol");

        assertEquals(MissionStep.BRIEFING, game.step());
        assertFalse(game.isUnlocked(MissionStep.SHIP));
        assertEquals(ShipBuild.empty(), game.build());
        assertEquals("ice-patrol", game.design().objective().id());
        assertTrue(game.design().instruments().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> game.selectMission("find-aliens"));
    }

    @Test
    void redesignFromTheReportKeepsTheParts() {
        GameSession game = sessionWithParts();
        game.next();
        game.next();
        chooseAllParts(game, "a");
        game.next();
        game.next();
        game.confirmBuild();
        game.next();
        game.markLaunchPassed();
        game.next();
        game.markSimulationFinished();
        game.next();
        ShipBuild before = game.build();

        game.redesign();

        assertEquals(MissionStep.SHIP, game.step());
        assertEquals(before, game.build());
        assertFalse(game.isUnlocked(MissionStep.LAUNCH));
    }

    @Test
    void theSunSynchronousOrbitCannotReachTheIceTargetButTheNearPolarOneCan() {
        double target = catalog.objective("ice-patrol").orElseThrow().targetLatitudeDeg();
        double sunSynchronousReach = OrbitMath.maxLatitudeDeg(catalog.orbit("sso-705").orElseThrow().inclinationDeg());
        double nearPolarReach = OrbitMath.maxLatitudeDeg(catalog.orbit("polar-600").orElseThrow().inclinationDeg());

        assertTrue(sunSynchronousReach < target);
        assertTrue(nearPolarReach >= target);
    }
}
