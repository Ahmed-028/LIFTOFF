package com.liftoff.data;

import com.liftoff.engine.OrbitMath;
import com.liftoff.engine.sim.EventCard;
import com.liftoff.model.Bus;
import com.liftoff.model.Catalog;
import com.liftoff.model.Instrument;
import com.liftoff.model.LaunchClass;
import com.liftoff.model.LaunchVehicle;
import com.liftoff.model.ModuleSlot;
import com.liftoff.model.Orbit;
import com.liftoff.model.ShipPart;
import com.liftoff.model.SourceRef;
import com.liftoff.model.ValueBasis;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Audits the bundled catalogs against the data rule (source and basis on everything) and against
 * docs/data-sources.md, figure by figure. A failure here means a catalog and its paper trail disagree.
 */
class CatalogAuditTest {

    private static final Path DATA_SOURCES = Path.of("docs", "data-sources.md");

    /** The phrase data-sources.md uses for each ship-part figure. Longest phrases first. */
    private static final Map<String, ToDoubleFunction<ShipPart>> PART_FIGURES = new LinkedHashMap<>();

    static {
        PART_FIGURES.put("m bay length", ShipPart::bayLengthM);
        PART_FIGURES.put("m bay width", ShipPart::bayDiameterM);
        PART_FIGURES.put("kg propellant", ShipPart::propellantCapacityKg);
        PART_FIGURES.put("kg payload", ShipPart::payloadMassKg);
        PART_FIGURES.put("kg mass", ShipPart::massKg);
        PART_FIGURES.put("m length", ShipPart::lengthM);
        PART_FIGURES.put("m width", ShipPart::diameterM);
        PART_FIGURES.put("W supplied", ShipPart::powerSupplyW);
        PART_FIGURES.put("W drawn", ShipPart::powerDrawW);
        PART_FIGURES.put("N thrust", ShipPart::thrustN);
        PART_FIGURES.put("s impulse", ShipPart::specificImpulseS);
    }

    private static Catalog catalog;
    private static String document;

    @BeforeAll
    static void load() throws IOException {
        catalog = CatalogLoader.loadFromClasspath();
        document = Files.readString(DATA_SOURCES, StandardCharsets.UTF_8);
    }

    // ---- the data rule: source and basis on every entry, unique ids, real quantities ----------

    @Test
    void everyEntryOfEveryCatalogHasASourceAndAValidBasis() {
        List<Record> entries = new ArrayList<>();
        entries.addAll(catalog.instruments());
        entries.addAll(catalog.buses());
        entries.addAll(catalog.orbits());
        entries.addAll(catalog.groundStations());
        entries.addAll(catalog.launchVehicles());
        entries.addAll(catalog.shipParts());
        entries.addAll(catalog.events());
        entries.addAll(catalog.objectives());
        assertFalse(entries.isEmpty());

        for (Record entry : entries) {
            int sources = 0;
            for (RecordComponent component : entry.getClass().getRecordComponents()) {
                Object value = read(entry, component);
                if (component.getType() == SourceRef.class) {
                    assertSource((SourceRef) value, entry.toString());
                    sources++;
                } else if (component.getType() == ValueBasis.class) {
                    assertNotNull(value, () -> component.getName() + " missing on " + entry);
                }
            }
            assertEquals(1, sources, () -> "exactly one source expected on " + entry);
        }
    }

    @Test
    void everyMissionSettingThatIsNotAScenarioChoiceHasItsOwnSourceAndBasis() {
        for (MissionEntry mission : catalog.missions()) {
            assertSource(mission.source(), mission.id());
            assertNotNull(mission.deltaVRequiredMs(), mission.id() + " delta-v");
            assertNotNull(mission.deltaVBasis(), mission.id() + " delta-v basis");
            assertSource(mission.deltaVSource(), mission.id() + " delta-v");
            if (mission.crew() > 0) {
                assertTrue(mission.suppliesKgPerCrewDay() > 0, mission.id());
                assertSource(mission.suppliesSource(), mission.id() + " supplies");
            }
            if (mission.downlinkMbps() > 0) {
                assertSource(mission.downlinkSource(), mission.id() + " downlink");
            }
        }
    }

    @Test
    void idsAreUniqueInEveryCatalog() {
        assertUnique("missions", catalog.missions().stream().map(MissionEntry::id).toList());
        assertUnique("ship parts", catalog.shipParts().stream().map(ShipPart::id).toList());
        assertUnique("instruments", catalog.instruments().stream().map(Instrument::id).toList());
        assertUnique("buses", catalog.buses().stream().map(Bus::id).toList());
        assertUnique("orbits", catalog.orbits().stream().map(Orbit::id).toList());
        assertUnique("stations", catalog.groundStations().stream().map(station -> station.id()).toList());
        assertUnique("rockets", catalog.launchVehicles().stream().map(LaunchVehicle::id).toList());
        assertUnique("events", catalog.events().stream().map(EventCard::id).toList());
    }

    @Test
    void noPhysicalValueIsZeroOrNegativeWhereThePartNeedsIt() {
        for (Instrument instrument : catalog.instruments()) {
            assertPositive(instrument.massKg(), instrument.id() + " mass");
            assertPositive(instrument.avgPowerW(), instrument.id() + " power");
            assertPositive(instrument.avgDataRateKbps(), instrument.id() + " data rate");
            assertFalse(instrument.capabilities().isEmpty(), instrument.id() + " measures nothing");
        }
        for (Bus bus : catalog.buses()) {
            assertPositive(bus.massKg(), bus.id() + " mass");
            assertPositive(bus.payloadMassKg(), bus.id() + " payload");
            assertPositive(bus.payloadPowerW(), bus.id() + " power");
            assertPositive(bus.downlinkMbps(), bus.id() + " downlink");
        }
        for (Orbit orbit : catalog.orbits()) {
            assertPositive(orbit.altitudeKm(), orbit.id() + " altitude");
            assertPositive(orbit.inclinationDeg(), orbit.id() + " inclination");
        }
        for (LaunchVehicle rocket : catalog.launchVehicles()) {
            assertPositive(rocket.costMusd(), rocket.id() + " cost");
            rocket.capacityKg().forEach((launchClass, kg) -> assertPositive(kg, rocket.id() + " " + launchClass));
        }
        for (ShipPart part : catalog.shipParts()) {
            String id = part.id();
            // The Shuttle bay is part of the orbiter, so data-sources.md gives it no mass of its own.
            if (!id.equals("shuttle-bay")) {
                assertPositive(part.massKg(), id + " mass");
                assertPositive(part.costMusd(), id + " cost");
            }
            switch (part.slot()) {
                case AIRFRAME -> assertPositive(part.payloadMassKg(), id + " payload");
                case THRUSTERS -> {
                    assertPositive(part.thrustN(), id + " thrust");
                    assertPositive(part.specificImpulseS(), id + " specific impulse");
                }
                case POWER -> assertPositive(part.powerSupplyW(), id + " power");
                case CARGO_BAY -> {
                    assertPositive(part.payloadMassKg(), id + " payload");
                    assertPositive(part.bayLengthM(), id + " bay length");
                    assertPositive(part.bayDiameterM(), id + " bay width");
                }
                case SATELLITE -> {
                    assertPositive(part.lengthM(), id + " length");
                    assertPositive(part.diameterM(), id + " width");
                }
            }
        }
    }

    // ---- the builder: enough options, and a model for every one of them -----------------------

    @Test
    void everyMissionOffersAtLeastFourOptionsInEachOfTheFiveSlots() {
        assertEquals(5, catalog.missions().size());
        for (MissionEntry mission : catalog.missions()) {
            for (ModuleSlot slot : ModuleSlot.values()) {
                int offered = catalog.partsFor(mission.id(), slot).size();
                assertTrue(offered >= 4, mission.id() + " offers only " + offered + " parts for " + slot);
            }
        }
    }

    @Test
    void everyModelIdResolvesToAnObjFile() {
        for (ShipPart part : catalog.shipParts()) {
            String resource = "/com/liftoff/models/" + part.modelId() + ".obj";
            assertNotNull(CatalogAuditTest.class.getResource(resource),
                    part.id() + " names a model that does not exist: " + resource);
        }
    }

    @Test
    void theThreeLadderMissionsRiseInDifficultyAndTheTwoExtrasAreOffTheLadder() {
        assertEquals(List.of("farm-watch", "moon-landing", "mars-drones"),
                catalog.missions().stream().filter(MissionEntry::ladder).map(MissionEntry::id).toList());
        assertEquals(List.of("EASY", "MEDIUM", "HARD"), catalog.missions().stream().filter(MissionEntry::ladder)
                .map(mission -> mission.difficulty().name()).toList());
        assertEquals(Set.of("storm-chasers", "ice-patrol"), new HashSet<>(catalog.missions().stream()
                .filter(mission -> !mission.ladder()).map(MissionEntry::id).toList()));
    }

    // ---- data-sources.md against the JSON, figure by figure -----------------------------------

    @Test
    void theHandbookInstrumentTableMatchesTheCatalog() {
        List<String[]> rows = table("## Instruments");
        assertEquals(10, rows.size());
        for (String[] row : rows) {
            Instrument instrument = catalog.instrument(row[0]).orElseThrow(() -> new AssertionError(row[0]));
            assertEquals(number(row[2]), instrument.massKg(), 0, row[0] + " mass");
            assertEquals(number(row[3]), instrument.avgPowerW(), 0, row[0] + " power");
            assertEquals(number(row[4]), instrument.avgDataRateKbps(), 0, row[0] + " data rate");
            assertEquals(row[3].contains("DERIVED") ? ValueBasis.DERIVED : ValueBasis.PUBLISHED,
                    instrument.powerBasis(), row[0] + " power basis");
            assertEquals(row[4].contains("DERIVED") ? ValueBasis.DERIVED : ValueBasis.PUBLISHED,
                    instrument.dataBasis(), row[0] + " data basis");
        }
    }

    @Test
    void theInstrumentsTheUserNamedCarryTheirPublishedFigures() {
        assertEquals(90, catalog.instrument("ali").orElseThrow().massKg());
        assertEquals(100, catalog.instrument("ali").orElseThrow().avgPowerW());
        assertEquals(229, catalog.instrument("modis").orElseThrow().massKg());
        assertEquals(162.5, catalog.instrument("modis").orElseThrow().avgPowerW());
        // Doc 05 section 6 rounds the same handbook figures.
        assertEquals(425, catalog.instrument("etm-plus").orElseThrow().massKg());
        assertEquals(421, catalog.instrument("aster").orElseThrow().massKg());
        assertEquals(463, catalog.instrument("aster").orElseThrow().avgPowerW());
        assertEquals(5.9, catalog.instrument("ali").orElseThrow().dataGbitPerDay(), 0.05);
        assertEquals(2164, catalog.instrument("etm-plus").orElseThrow().dataGbitPerDay(), 0.5);
        assertEquals(717, catalog.instrument("aster").orElseThrow().dataGbitPerDay(), 0.5);
        assertEquals(536, catalog.instrument("modis").orElseThrow().dataGbitPerDay(), 0.5);
    }

    @Test
    void theAddedInstrumentTableMatchesTheCatalog() {
        List<String[]> rows = table("## Added instruments");
        assertEquals(9, rows.size());
        Pattern figures = Pattern.compile("([\\d.]+) kg, ([\\d.]+) W, ([\\d.]+) kbps");
        for (String[] row : rows) {
            Instrument instrument = catalog.instrument(row[0]).orElseThrow(() -> new AssertionError(row[0]));
            Matcher match = figures.matcher(row[1]);
            assertTrue(match.find(), row[1]);
            assertRounded(match.group(1), instrument.massKg(), row[0] + " mass");
            assertRounded(match.group(2), instrument.avgPowerW(), row[0] + " power");
            assertRounded(match.group(3), instrument.avgDataRateKbps(), row[0] + " data rate");
            String[] bases = row[2].split("/");
            assertEquals(basis(bases[0]), instrument.powerBasis(), row[0] + " power basis");
            assertEquals(basis(bases[1]), instrument.dataBasis(), row[0] + " data basis");
            assertEquals(ValueBasis.GAME_ESTIMATE, instrument.costBasis(), row[0]);
            // "All new instruments use the existing $0.5M/kg cost": a game estimate, checked as arithmetic.
            assertEquals(0.5 * instrument.massKg(), instrument.costMusd(), 1e-9, row[0] + " cost");
        }
    }

    @Test
    void theBusTableMatchesTheCatalog() {
        List<String[]> rows = table("## Spacecraft buses");
        assertEquals(3, rows.size());
        for (String[] row : rows) {
            Bus bus = catalog.bus(row[0]).orElseThrow();
            assertEquals(number(row[2]), bus.massKg(), 0, row[0] + " mass");
            assertEquals(number(row[3]), bus.payloadMassKg(), 0, row[0] + " payload");
            assertEquals(number(row[4]), bus.payloadPowerW(), 0, row[0] + " payload power");
            assertEquals(number(row[5]), bus.downlinkMbps(), 0, row[0] + " downlink");
            // The same bus is offered as an airframe in the builder: the two files must agree.
            ShipPart airframe = catalog.shipPart(row[0]).orElseThrow();
            assertEquals(bus.massKg(), airframe.massKg(), 0, row[0]);
            assertEquals(bus.payloadMassKg(), airframe.payloadMassKg(), 0, row[0]);
        }
        assertEquals(48, catalog.bus("eo1-class").orElseThrow().storageGbit());
        assertEquals(380, catalog.bus("landsat7-class").orElseThrow().storageGbit());
        assertEquals(null, catalog.bus("sorce-class").orElseThrow().storageGbit(), "still to source: never guessed");
    }

    @Test
    void theOrbitTableMatchesTheCatalog() {
        List<String[]> rows = table("## Orbits");
        assertEquals(3, rows.size());
        for (String[] row : rows) {
            Orbit orbit = catalog.orbit(row[0]).orElseThrow();
            assertEquals(number(row[2]), orbit.altitudeKm(), 0, row[0] + " altitude");
            assertEquals(number(row[3]), orbit.inclinationDeg(), 0, row[0] + " inclination");
        }
    }

    @Test
    void theRocketFiguresMatchTheCatalog() {
        assertRocket("electron", 7.5, Map.of(LaunchClass.LOW_INCLINATION, 300.0, LaunchClass.POLAR, 200.0));
        assertRocket("pegasus-xl", 56.3, Map.of(LaunchClass.LOW_INCLINATION, 453.59, LaunchClass.POLAR, 221.0));
        assertRocket("falcon-9", 80.4, Map.of(LaunchClass.LOW_INCLINATION, 22_800.0, LaunchClass.POLAR, 1700.0,
                LaunchClass.TRANS_MARS, 4020.0));
        assertRocket("atlas-v-401", 153.8, Map.of(LaunchClass.LOW_INCLINATION, 9800.0, LaunchClass.POLAR, 8080.0));
        assertRocket("sls-block-1", 2200, Map.of(LaunchClass.TRANS_LUNAR, 27_000.0));
        assertRocket("atlas-v-541", 243, Map.of(LaunchClass.TRANS_MARS, 3893.0));
        assertRocket("saturn-v", 185, Map.of(LaunchClass.TRANS_LUNAR, 43_500.0));
        assertEquals(7, catalog.launchVehicles().size(), "a rocket was added without a row here");

        // The document must still say the same thing.
        for (String figure : List.of("300 low-inclination LEO; 200 sun-synchronous", "453.59 LEO",
                "22 800 LEO at 28.5°", "9 800 LEO at 28.7°; 8 080 at 90°", "221 kg POLAR", "1700 kg POLAR",
                "4020 kg TRANS_MARS", "27000 kg TRANS_LUNAR", "3893 kg TRANS_MARS", "43500 kg TRANS_LUNAR")) {
            assertTrue(document.contains(figure), "data-sources.md no longer states: " + figure);
        }
    }

    @Test
    void everyShipPartHasARowAndEveryFigureInItMatchesTheCatalog() {
        List<String[]> rows = table("## Ship parts");
        Set<String> documented = new HashSet<>();
        Pattern figure = Pattern.compile("([\\d.]+) (" + String.join("|", PART_FIGURES.keySet()) + ")");
        for (String[] row : rows) {
            ShipPart part = catalog.shipPart(row[0]).orElseThrow(() -> new AssertionError("no part " + row[0]));
            documented.add(row[0]);
            Matcher match = figure.matcher(row[1]);
            int found = 0;
            while (match.find()) {
                assertRounded(match.group(1), PART_FIGURES.get(match.group(2)).applyAsDouble(part),
                        row[0] + " " + match.group(2));
                found++;
            }
            assertEquals(row[1].split(",").length, found, "unread figure in the row of " + row[0] + ": " + row[1]);
            // "All part costs use the existing bus estimate of $0.15M/kg": a game estimate, checked as arithmetic.
            assertEquals(ValueBasis.GAME_ESTIMATE, part.costBasis(), row[0]);
            assertEquals(0.15 * part.massKg(), part.costMusd(), 1e-6, row[0] + " cost");
        }
        assertEquals(catalog.shipParts().stream().map(ShipPart::id).collect(java.util.stream.Collectors.toSet()),
                documented, "every ship part needs a row in data-sources.md");
    }

    @Test
    void theMissionSettingsTableMatchesTheCatalog() {
        List<String[]> rows = table("## Mission settings");
        assertEquals(5, rows.size());
        Pattern settings = Pattern.compile("(\\d+) \\$M, (\\d+) months to launch, (\\d+) minimum");
        Pattern deltaV = Pattern.compile("^(\\d+) m/s \\((\\w+)\\)");
        for (String[] row : rows) {
            MissionEntry mission = catalog.mission(row[0]).orElseThrow();
            Matcher caps = settings.matcher(row[1]);
            assertTrue(caps.find(), row[1]);
            assertEquals(number(caps.group(1)), mission.briefing().budgetCapMusd(), 0, row[0] + " budget");
            assertEquals(number(caps.group(2)), mission.briefing().deadlineMonths(), 0, row[0] + " deadline");
            assertEquals(number(caps.group(3)), mission.briefing().minScienceMonths(), 0, row[0] + " science");
            Matcher trip = deltaV.matcher(row[2]);
            assertTrue(trip.find(), row[2]);
            assertEquals(number(trip.group(1)), mission.deltaVRequiredMs(), 0, row[0] + " delta-v");
            assertEquals(ValueBasis.valueOf(trip.group(2)), mission.deltaVBasis(), row[0] + " delta-v basis");
        }
        MissionEntry moon = catalog.mission("moon-landing").orElseThrow();
        // The legs the document sums.
        assertEquals(60 + 915 + 2165 + 1920 + 915 + 60, moon.deltaVRequiredMs(), 0);
        assertEquals(2165 + 1920, moon.surfaceDeltaVRequiredMs(), 0);
        assertEquals(0.84 + 0.62 + 1.15 + 0.76 + 1.62, moon.suppliesKgPerCrewDay(), 1e-9);
        assertEquals(4.99, moon.suppliesKgPerCrewDay(), 0);
        assertEquals(80, moon.downlinkMbps(), 0);
        assertEquals(8, moon.steps());
        assertEquals(2, moon.crew());
        MissionEntry mars = catalog.mission("mars-drones").orElseThrow();
        assertEquals(80, mars.surfaceDeltaVRequiredMs(), 0);
        assertEquals(2, mars.downlinkMbps(), 0);
        assertEquals(6, mars.steps());
    }

    @Test
    void everyEventInTheDocumentIsInTheCatalogWithItsRealExample() {
        List<String[]> rows = table("## Flight events");
        assertEquals(catalog.events().size(), rows.size());
        for (String[] row : rows) {
            EventCard event = catalog.events().stream().filter(card -> card.id().equals(row[0])).findFirst()
                    .orElseThrow(() -> new AssertionError(row[0]));
            assertEquals(row[1], event.realExample(), row[0]);
        }
    }

    // ---- orbit arithmetic stated in doc 06 section 4 ------------------------------------------

    @Test
    void the705KmOrbitTakes98Point8MinutesAndSvalbardSeesItAbout137MinutesADay() {
        Orbit sso = catalog.orbit("sso-705").orElseThrow();
        Orbit tropical = catalog.orbit("tropical-402").orElseThrow();
        double svalbardDeg = catalog.groundStation("svalbard").orElseThrow().latitudeDeg();

        // The formula gives 98.88 min; the docs quote it as 98.8.
        assertEquals(98.8, OrbitMath.periodMinutes(sso.altitudeKm()), 0.1);
        assertEquals(137, OrbitMath.contactMinutesPerDay(sso, svalbardDeg), 1.0);
        assertEquals(0, OrbitMath.contactMinutesPerDay(tropical, svalbardDeg), 0);
    }

    // ---- helpers ----------------------------------------------------------------------------

    /** The data rows of the first table under the heading, as trimmed cells. */
    private static List<String[]> table(String heading) {
        int start = document.indexOf("\n" + heading);
        assertTrue(start >= 0, "data-sources.md has no section " + heading);
        List<String[]> rows = new ArrayList<>();
        boolean inTable = false;
        for (String line : document.substring(start + 1).split("\\R")) {
            if (line.startsWith("|")) {
                if (!inTable) {
                    inTable = true;          // header row
                } else if (!line.matches("\\|[\\s|:-]+\\|")) {
                    String[] cells = line.substring(1, line.lastIndexOf('|')).split("\\|");
                    for (int i = 0; i < cells.length; i++) {
                        cells[i] = cells[i].trim();
                    }
                    rows.add(cells);
                }
            } else if (inTable) {
                break;
            }
        }
        assertFalse(rows.isEmpty(), "no table under " + heading);
        return rows;
    }

    /** The leading number of a cell such as "25 050 DERIVED", "248.4 = 315 − 66.6", "98.2°" or "105 (X-band)". */
    private static double number(String cell) {
        Matcher match = Pattern.compile("^(\\d+(?: \\d{3})*(?:\\.\\d+)?)").matcher(cell);
        assertTrue(match.find(), "no number in: " + cell);
        return Double.parseDouble(match.group(1).replace(" ", ""));
    }

    /** The document prints a catalog value rounded to the digits shown: they must agree to those digits. */
    private static void assertRounded(String printed, double catalogValue, String what) {
        int decimals = printed.contains(".") ? printed.length() - printed.indexOf('.') - 1 : 0;
        // Six significant figures is the most the document prints (15094.6 for 15094.65).
        double tolerance = Math.max(0.5 * Math.pow(10, -decimals), 0) + 1e-9;
        assertEquals(Double.parseDouble(printed), catalogValue, tolerance, what);
    }

    private static ValueBasis basis(String cell) {
        for (ValueBasis basis : ValueBasis.values()) {
            if (cell.trim().startsWith(basis.name())) {
                return basis;
            }
        }
        return fail("no basis in: " + cell);
    }

    private static void assertRocket(String id, double costMusd, Map<LaunchClass, Double> capacityKg) {
        LaunchVehicle rocket = catalog.launchVehicle(id).orElseThrow();
        assertEquals(costMusd, rocket.costMusd(), 0, id + " cost");
        assertEquals(capacityKg, rocket.capacityKg(), id + " capacity");
    }

    private static void assertSource(SourceRef source, String where) {
        assertNotNull(source, where + " has no source");
        assertFalse(source.title() == null || source.title().isBlank(), where + " source has no title");
        assertFalse(source.detail() == null || source.detail().isBlank(), where + " source has no detail");
        assertTrue(source.url() != null && source.url().startsWith("https://"), where + " source link: " + source.url());
    }

    private static void assertUnique(String catalogName, List<String> ids) {
        assertFalse(ids.isEmpty(), catalogName + " is empty");
        assertEquals(ids.size(), new HashSet<>(ids).size(), "duplicate id in " + catalogName + ": " + ids);
        ids.forEach(id -> assertFalse(id == null || id.isBlank(), "blank id in " + catalogName));
    }

    private static void assertPositive(double value, String what) {
        assertTrue(Double.isFinite(value) && value > 0, what + " must be above zero, was " + value);
    }

    private static Object read(Record entry, RecordComponent component) {
        try {
            return component.getAccessor().invoke(entry);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
