package com.liftoff.data;

import com.liftoff.TestParts;
import com.liftoff.engine.OrbitMath;
import com.liftoff.engine.sim.EventDeck;
import com.liftoff.engine.sim.MissionSimulator;
import com.liftoff.engine.sim.SimEventType;
import com.liftoff.engine.sim.SimLimit;
import com.liftoff.model.Bus;
import com.liftoff.model.Capability;
import com.liftoff.model.Catalog;
import com.liftoff.model.Difficulty;
import com.liftoff.model.GroundStation;
import com.liftoff.model.Instrument;
import com.liftoff.model.LaunchClass;
import com.liftoff.model.LaunchVehicle;
import com.liftoff.model.MissionDesign;
import com.liftoff.model.ModuleSlot;
import com.liftoff.model.Objective;
import com.liftoff.model.Orbit;
import com.liftoff.model.Requirement;
import com.liftoff.model.ShipPart;
import com.liftoff.model.SourceRef;
import com.liftoff.model.ValueBasis;
import com.liftoff.session.GameSession;
import com.liftoff.session.MissionStep;
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
    void allBundledCatalogsLoad() {
        assertEquals(3, catalog.objectives().size());
        assertTrue(catalog.instruments().size() > 10);
        assertEquals(3, catalog.buses().size());
        assertEquals(3, catalog.orbits().size());
        assertEquals(6, catalog.groundStations().size());
        assertEquals(5, catalog.missions().size());
        assertEquals(List.of("farm-watch", "moon-landing", "mars-drones"), catalog.missions().stream()
                .filter(MissionEntry::ladder).map(MissionEntry::id).toList());
        for (MissionEntry mission : catalog.missions()) {
            for (ModuleSlot slot : ModuleSlot.values()) {
                assertTrue(catalog.partsFor(mission.id(), slot).size() >= 4, mission.id() + " / " + slot);
            }
        }
        assertTrue(catalog.launchVehicle("sls-block-1").orElseThrow().capacityFor(LaunchClass.TRANS_LUNAR).isPresent());
        assertTrue(catalog.launchVehicle("atlas-v-541").orElseThrow().capacityFor(LaunchClass.TRANS_MARS).isPresent());
        assertEquals(catalog.events(), EventDeck.fromClasspath().cards());
        for (SimEventType type : SimEventType.values()) {
            assertTrue(catalog.events().stream().anyMatch(card -> card.type() == type), type.name());
        }
    }

    @Test
    void validationRejectsTooFewPartsForAMissionSlot() {
        List<ShipPart> parts = catalog.shipParts().stream()
                .filter(part -> part.slot() != ModuleSlot.THRUSTERS).toList();
        assertTrue(assertThrows(CatalogException.class, () -> CatalogLoader.validate(withParts(parts)))
                .getMessage().contains("THRUSTERS"));
    }

    @Test
    void bundledShipPartsCanFlyFarmWatchThroughTheGameSession() {
        GameSession session = new GameSession(catalog);
        session.selectMission("farm-watch");
        session.next();
        session.choosePart("eo1-class");
        session.choosePart("mr-103");
        session.choosePart("sp6x-wings");
        session.choosePart("bay-3u");
        session.choosePart("cubesat-3u");
        assertEquals(catalog.shipPart("cubesat-3u"), session.build().part(ModuleSlot.SATELLITE));
        session.next();
        session.chooseBus("eo1-class");
        session.addInstrument("ali");
        session.chooseOrbit("sso-705");
        session.toggleStation("svalbard");
        session.toggleStation("fairbanks");
        session.chooseLaunchVehicle("atlas-v-401");
        session.next();
        session.confirmBuild();
        session.next();

        assertTrue(session.runLaunchCheck().isEmpty());
        session.next();
        assertTrue(session.fly(2026).success());
        session.next();
        assertEquals(MissionStep.REPORT, session.step());
    }

    @Test
    void busRecorderCapacitiesAreAvailableWhereSourced() {
        assertEquals(380.0, catalog.bus("landsat7-class").orElseThrow().storageGbit());
        assertEquals(48.0, catalog.bus("eo1-class").orElseThrow().storageGbit());
    }

    @Test
    void defaultSimulationUsesTheCatalogRecorderCapacity() {
        Bus bus = catalog.bus("eo1-class").orElseThrow();
        MissionDesign design = MissionDesign.empty().withBus(bus)
                .plusInstrument(catalog.instrument("ali").orElseThrow()).withScienceMonths(1);
        var result = MissionSimulator.run(design, com.liftoff.engine.sim.SimFixtures.mission("farm-watch", 0), Difficulty.EASY, 1);

        assertEquals(48, result.storedAtEndGbit(), 1e-9);
        assertEquals(result.totalProducedGbit() - 48, result.totalLostGbit(), 1e-9);
        assertEquals(SimLimit.STORAGE, result.firstLimit());
    }

    @Test
    void sessionUsesTheCatalogBriefingAndFlightSettings() {
        GameSession session = new GameSession(catalog);
        for (String id : List.of("moon-landing", "mars-drones", "farm-watch")) {
            MissionEntry entry = catalog.mission(id).orElseThrow();
            session.selectMission(id);

            assertEquals(entry.briefing(), session.design().objective());
            assertEquals(entry.difficulty(), session.difficulty());
            assertEquals(entry.deltaVRequiredMs(), session.buildReport().deltaVRequiredMs());
        }
    }

    @Test
    void sessionRejectsPartsForAnotherMissionWithoutChangingTheBuild() {
        GameSession session = new GameSession(catalog);
        session.selectMission("farm-watch");
        session.choosePart("cubesat-3u");

        assertThrows(IllegalArgumentException.class, () -> session.choosePart("perseverance"));
        assertEquals(catalog.shipPart("cubesat-3u"), session.build().part(ModuleSlot.SATELLITE));
    }

    @Test
    void cubeSatBaysUseThePublishedDeployerMass() {
        assertEquals(2, catalog.shipPart("bay-3u").orElseThrow().massKg());
        assertEquals(4.5, catalog.shipPart("bay-6u").orElseThrow().massKg());
        assertEquals(7.5, catalog.shipPart("bay-12u").orElseThrow().massKg());
    }

    @Test
    void validationRejectsABlankModelId() {
        assertInvalidPart(ModuleSlot.POWER, " ", 1, "modelId");
    }

    @Test
    void validationRejectsNonpositiveDimensionsWhereUsed() {
        assertInvalidPart(ModuleSlot.CARGO_BAY, "model", 0, "bay length");
        assertInvalidPart(ModuleSlot.SATELLITE, "model", 0, "payload length");
    }

    @Test
    void aNegativeFigureInAPartFileIsACatalogError() {
        // The real ShipPart refuses a negative figure itself; the loader must report it, not crash.
        String json = "[{\"id\":\"bad\",\"slot\":\"SATELLITE\",\"name\":\"n\",\"kidName\":\"k\","
                + "\"description\":\"d\",\"basedOn\":\"b\",\"missions\":[\"farm-watch\"],\"massKg\":-1,"
                + "\"powerSupplyW\":0,\"powerDrawW\":0,\"payloadMassKg\":0,\"bayLengthM\":0,\"bayDiameterM\":0,"
                + "\"lengthM\":1,\"diameterM\":1,\"thrustN\":0,\"specificImpulseS\":0,\"propellantCapacityKg\":0,"
                + "\"dataGbitPerDay\":0,\"costMusd\":0,\"costBasis\":\"GAME_ESTIMATE\",\"modelId\":\"m\","
                + "\"source\":{\"title\":\"t\",\"detail\":\"d\",\"url\":\"u\"}}]";
        assertThrows(CatalogException.class, () -> CatalogLoader.parseList(json, ShipPart.class));
    }

    private static void assertInvalidPart(ModuleSlot slot, String modelId, double length, String problem) {
        List<ShipPart> parts = new ArrayList<>(catalog.shipParts());
        ShipPart template = catalog.partsFor("farm-watch", slot).getFirst();
        parts.set(parts.indexOf(template), new ShipPart(template.id(), slot, template.name(), template.kidName(),
                template.description(), template.basedOn(), template.missions(), template.massKg(),
                template.powerSupplyW(), template.powerDrawW(), template.payloadMassKg(), length,
                template.bayDiameterM(), length, template.diameterM(), template.thrustN(),
                template.specificImpulseS(), template.propellantCapacityKg(), template.dataGbitPerDay(),
                template.costMusd(), template.costBasis(), modelId, template.source()));
        assertTrue(assertThrows(CatalogException.class, () -> CatalogLoader.validate(withParts(parts)))
                .getMessage().contains(problem));
    }

    private static Catalog withParts(List<ShipPart> parts) {
        return new Catalog(catalog.objectives(), catalog.instruments(), catalog.buses(), catalog.orbits(),
                catalog.groundStations(), catalog.launchVehicles(), catalog.missions(), parts, catalog.events());
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
