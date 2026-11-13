package com.liftoff.data;

import com.liftoff.TestParts;
import com.liftoff.engine.OrbitMath;
import com.liftoff.model.Bus;
import com.liftoff.model.Capability;
import com.liftoff.model.Catalog;
import com.liftoff.model.GroundStation;
import com.liftoff.model.Instrument;
import com.liftoff.model.LaunchVehicle;
import com.liftoff.model.Objective;
import com.liftoff.model.Orbit;
import com.liftoff.model.Requirement;
import com.liftoff.model.SourceRef;
import com.liftoff.model.ValueBasis;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogLoaderTest {

    private static Catalog catalog;

    @BeforeAll
    static void loadTheBundledCatalogs() {
        catalog = CatalogLoader.loadFromClasspath();
    }

    @Test
    void allSixCatalogsLoad() {
        assertEquals(3, catalog.objectives().size());
        assertEquals(10, catalog.instruments().size());
        assertEquals(3, catalog.buses().size());
        assertEquals(3, catalog.orbits().size());
        assertEquals(6, catalog.groundStations().size());
        assertEquals(4, catalog.launchVehicles().size());
    }

    @Test
    void entriesCanBeLookedUpById() {
        assertEquals("MODIS", catalog.instrument("modis").orElseThrow().name());
        assertEquals("Ice Patrol", catalog.objective("ice-patrol").orElseThrow().title());
        assertTrue(catalog.bus("eo1-class").isPresent());
        assertTrue(catalog.orbit("sso-705").isPresent());
        assertTrue(catalog.groundStation("svalbard").isPresent());
        assertTrue(catalog.launchVehicle("atlas-v-401").isPresent());
    }

    @Test
    void anUnknownIdGivesNothing() {
        assertTrue(catalog.instrument("warp-drive").isEmpty());
    }

    @Test
    void idsAreUniqueWithinEachCatalog() {
        assertUnique(catalog.objectives(), Objective::id);
        assertUnique(catalog.instruments(), Instrument::id);
        assertUnique(catalog.buses(), Bus::id);
        assertUnique(catalog.orbits(), Orbit::id);
        assertUnique(catalog.groundStations(), GroundStation::id);
        assertUnique(catalog.launchVehicles(), LaunchVehicle::id);
    }

    @Test
    void everyEntryNamesItsSource() {
        List<SourceRef> sources = new ArrayList<>();
        catalog.objectives().forEach(entry -> sources.add(entry.source()));
        catalog.instruments().forEach(entry -> sources.add(entry.source()));
        catalog.buses().forEach(entry -> sources.add(entry.source()));
        catalog.orbits().forEach(entry -> sources.add(entry.source()));
        catalog.groundStations().forEach(entry -> sources.add(entry.source()));
        catalog.launchVehicles().forEach(entry -> sources.add(entry.source()));

        for (SourceRef source : sources) {
            assertNotNull(source);
            assertFalse(source.title().isBlank());
            assertTrue(source.url().startsWith("https://"), source.url());
        }
    }

    @Test
    void modisCarriesTheHandbookFigures() {
        // NASA Earth Science Reference Handbook (2006), Key MODIS Facts.
        Instrument modis = catalog.instrument("modis").orElseThrow();

        assertEquals(229, modis.massKg(), 1e-9);
        assertEquals(162.5, modis.avgPowerW(), 1e-9);
        assertEquals(6200, modis.avgDataRateKbps(), 1e-9);
    }

    @Test
    void everyMeasurementAnObjectiveRequiresCanBeMadeBySomeInstrument() {
        for (Objective objective : catalog.objectives()) {
            for (Requirement requirement : objective.required()) {
                boolean offered = catalog.instruments().stream()
                        .flatMap(instrument -> instrument.capabilities().stream())
                        .anyMatch(capability -> meets(capability, requirement));
                assertTrue(offered, objective.id() + " needs " + requirement);
            }
        }
    }

    @Test
    void everyObjectiveCanBeReachedBySomeOrbit() {
        for (Objective objective : catalog.objectives()) {
            boolean reachable = catalog.orbits().stream()
                    .anyMatch(orbit -> OrbitMath.maxLatitudeDeg(orbit.inclinationDeg())
                            >= objective.targetLatitudeDeg());
            assertTrue(reachable, objective.id());
        }
    }

    @Test
    void everyOrbitHasAtLeastOneRocketWithAPublishedCapacity() {
        for (Orbit orbit : catalog.orbits()) {
            boolean launchable = catalog.launchVehicles().stream()
                    .anyMatch(rocket -> rocket.capacityFor(orbit.launchClass()).isPresent());
            assertTrue(launchable, orbit.id());
        }
    }

    @Test
    void parsingRejectsAFieldTheModelDoesNotKnow() {
        String json = """
                [ { "title": "A", "detail": "B", "url": "https://example.test", "colour": "blue" } ]
                """;

        assertThrows(CatalogException.class, () -> CatalogLoader.parseList(json, SourceRef.class));
    }

    @Test
    void parsingReadsAListOfEntries() {
        String json = """
                [ { "title": "A", "detail": "B", "url": "https://example.test" } ]
                """;

        List<SourceRef> sources = CatalogLoader.parseList(json, SourceRef.class);

        assertEquals(List.of(new SourceRef("A", "B", "https://example.test")), sources);
    }

    @Test
    void validationRejectsAnEntryWithoutASource() {
        Instrument template = TestParts.instrument("no-source", 10, 10, 10, 5, 18);
        Instrument withoutSource = new Instrument(template.id(), template.name(), template.kidName(),
                template.description(), template.flewOn(), template.massKg(), template.avgPowerW(),
                template.powerBasis(), template.avgDataRateKbps(), template.dataBasis(),
                template.capabilities(), template.costMusd(), template.costBasis(),
                template.buildMonths(), template.buildBasis(), null);

        assertThrows(CatalogException.class, () -> CatalogLoader.validate(catalogWith(withoutSource)));
    }

    @Test
    void validationRejectsTwoEntriesWithTheSameId() {
        Instrument first = TestParts.instrument("twin", 10, 10, 10, 5, 18);
        Instrument second = TestParts.instrument("twin", 20, 20, 20, 10, 18);

        assertThrows(CatalogException.class, () -> CatalogLoader.validate(catalogWith(first, second)));
    }

    @Test
    void validationRejectsAMassThatIsNotPositive() {
        Instrument weightless = TestParts.instrument("weightless", 0, 10, 10, 5, 18);

        assertThrows(CatalogException.class, () -> CatalogLoader.validate(catalogWith(weightless)));
    }

    @Test
    void validationAcceptsAWellFormedCatalog() {
        CatalogLoader.validate(catalogWith(TestParts.instrument("fine", 10, 10, 10, 5, 18)));
    }

    @Test
    void costsOfBusesAndInstrumentsAreMarkedAsGameEstimates() {
        // NASA does not publish per-component prices, so none of these may claim to be published.
        catalog.instruments().forEach(entry -> assertEquals(ValueBasis.GAME_ESTIMATE, entry.costBasis(), entry.id()));
        catalog.buses().forEach(entry -> assertEquals(ValueBasis.GAME_ESTIMATE, entry.costBasis(), entry.id()));
    }

    private static Catalog catalogWith(Instrument... instruments) {
        return new Catalog(List.of(), List.of(instruments), List.of(), List.of(), List.of(), List.of());
    }

    private static boolean meets(Capability capability, Requirement requirement) {
        if (capability.type() != requirement.type()) {
            return false;
        }
        if (requirement.maxResolutionM() == null) {
            return true;
        }
        return capability.resolutionM() != null && capability.resolutionM() <= requirement.maxResolutionM();
    }

    private static <T> void assertUnique(List<T> entries, Function<T, String> idOf) {
        HashSet<String> seen = new HashSet<>();
        for (T entry : entries) {
            assertTrue(seen.add(idOf.apply(entry)), "duplicate id " + idOf.apply(entry));
        }
    }
}
