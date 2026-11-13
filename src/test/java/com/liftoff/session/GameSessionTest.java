package com.liftoff.session;

import com.liftoff.data.CatalogLoader;
import com.liftoff.engine.OrbitMath;
import com.liftoff.engine.ResourceLevel;
import com.liftoff.engine.ResourceReport;
import com.liftoff.engine.ResourceType;
import com.liftoff.model.Catalog;
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

        assertEquals(1857.0, report.get(ResourceType.MASS).used(), 1e-9);          // 1557 + 300
        assertEquals(8080.0, report.get(ResourceType.MASS).capacity().orElseThrow(), 1e-9);
        assertEquals(330.0, report.get(ResourceType.POWER).used(), 1e-9);
        assertEquals(553.55, report.get(ResourceType.BUDGET).used(), 1e-6);        // 233.55+150+153.8+16.2
        assertEquals(86.0, report.get(ResourceType.TIME).used(), 1e-9);            // 42 + 6 + 2 + 36
        assertFalse(report.anyOver(), report.toString());
    }

    @Test
    void aSmallStormSpacecraftBarelyFitsTheSmallestRocket() {
        session.selectObjective("storm-chasers");
        session.chooseBus("sorce-class");
        session.addInstrument("virs");
        session.chooseOrbit("tropical-402");
        session.chooseLaunchVehicle("electron");

        // 248.4 + 48.6 = 297.0 kg against Electron's 300 kg
        assertEquals(297.0, session.resources().get(ResourceType.MASS).used(), 1e-9);
        assertEquals(ResourceLevel.TIGHT, session.resources().get(ResourceType.MASS).level());
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
