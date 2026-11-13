package com.liftoff.session;

import com.liftoff.data.CatalogLoader;
import com.liftoff.engine.BuildIssue;
import com.liftoff.engine.FuelMath;
import com.liftoff.engine.ResourceType;
import com.liftoff.engine.score.Report;
import com.liftoff.engine.score.ReportBuilder;
import com.liftoff.engine.sim.LaunchIssue;
import com.liftoff.model.Catalog;
import com.liftoff.model.MissionDesign;
import com.liftoff.model.ModuleSlot;
import com.liftoff.model.ShipBuild;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plays all five missions from briefing to report through GameSession, with the real catalog and
 * the real ship parts: one design that succeeds and one that fails in each distinct way, and checks
 * that the game names the right first broken limit.
 */
class EveryMissionTest {

    private static final long SEED = 7;

    private static Catalog catalog;

    @BeforeAll
    static void loadCatalog() {
        catalog = CatalogLoader.loadFromClasspath();
    }

    /** A design, written the way the player makes it. Null or empty means "not chosen". */
    private record Plan(String mission, List<String> parts, List<String> instruments, String orbit,
                        List<String> stations, String rocket, double suppliesKg, int scienceMonths) {

        Plan withParts(String... ids) {
            return new Plan(mission, List.of(ids), instruments, orbit, stations, rocket, suppliesKg, scienceMonths);
        }

        Plan withInstruments(String... ids) {
            return new Plan(mission, parts, List.of(ids), orbit, stations, rocket, suppliesKg, scienceMonths);
        }

        Plan withOrbit(String id) {
            return new Plan(mission, parts, instruments, id, stations, rocket, suppliesKg, scienceMonths);
        }

        Plan withStations(String... ids) {
            return new Plan(mission, parts, instruments, orbit, List.of(ids), rocket, suppliesKg, scienceMonths);
        }

        Plan withRocket(String id) {
            return new Plan(mission, parts, instruments, orbit, stations, id, suppliesKg, scienceMonths);
        }

        Plan withSupplies(double kg) {
            return new Plan(mission, parts, instruments, orbit, stations, rocket, kg, scienceMonths);
        }

        Plan withScienceMonths(int months) {
            return new Plan(mission, parts, instruments, orbit, stations, rocket, suppliesKg, months);
        }
    }

    private static final Plan FARM = new Plan("farm-watch",
            List.of("eo1-class", "mr-106", "pumpkin-clamshell", "bay-6u", "cubesat-3u"),
            List.of("ali"), "sso-705", List.of("svalbard", "fairbanks"), "atlas-v-401", 0, 0);

    private static final Plan MOON = new Plan("moon-landing",
            List.of("apollo-service-module", "rl10c-1", "shuttle-fuel-cell", "apollo-11-adapter", "apollo-11-lander"),
            List.of("lola"), null, List.of("wallops"), "saturn-v", 80, 0);

    private static final Plan MARS = new Plan("mars-drones",
            List.of("curiosity-cruise", "mr-106", "mmrtg", "mars-2020-entry", "perseverance"),
            List.of("mastcam-z", "supercam"), null, List.of("wallops"), "atlas-v-541", 0, 0);

    private static final Plan STORM = new Plan("storm-chasers",
            List.of("sorce-class", "mr-106", "pumpkin-clamshell", "bay-6u", "cubesat-3u"),
            List.of("tmi"), "tropical-402", List.of("singapore", "south-point"), "pegasus-xl", 0, 0);

    private static final Plan ICE = new Plan("ice-patrol",
            List.of("landsat7-class", "mr-106", "shuttle-fuel-cell", "bay-6u", "cubesat-3u"),
            List.of("glas"), "polar-600", List.of("svalbard", "mcmurdo"), "atlas-v-401", 0, 0);

    /** Briefing, Ship and Supplies, ending on the Review step. */
    private static GameSession design(Plan plan) {
        GameSession session = new GameSession(catalog);
        session.selectMission(plan.mission());
        session.next();
        plan.parts().forEach(session::choosePart);
        session.next();
        plan.instruments().forEach(session::addInstrument);
        if (plan.orbit() != null) {
            session.chooseOrbit(plan.orbit());
        }
        plan.stations().forEach(session::toggleStation);
        if (plan.rocket() != null) {
            session.chooseLaunchVehicle(plan.rocket());
        }
        if (plan.suppliesKg() > 0) {
            session.setCrewSuppliesKg(plan.suppliesKg());
        }
        if (plan.scienceMonths() > 0) {
            session.setScienceMonths(plan.scienceMonths());
        }
        session.next();
        assertEquals(MissionStep.REVIEW, session.step());
        return session;
    }

    /** Confirm, launch check and flight; stops where the game stops the player. */
    private static GameSession play(Plan plan) {
        GameSession session = design(plan);
        if (!session.buildReport().canConfirm()) {
            return session;
        }
        session.confirmBuild();
        session.next();
        if (!session.runLaunchCheck().isEmpty()) {
            return session;
        }
        session.next();
        session.fly(SEED);
        session.next();
        return session;
    }

    private static void assertFlown(GameSession session, String brokeFirst) {
        assertEquals(MissionStep.REPORT, session.step(),
                () -> "build " + session.buildReport().issues() + " launch " + session.launchIssues());
        assertEquals(brokeFirst, session.report().orElseThrow().brokeFirst().messageKey());
    }

    private static void assertSucceeds(Plan plan) {
        GameSession session = play(plan);
        assertFlown(session, ReportBuilder.BROKE_NOTHING);
        assertTrue(session.simulation().orElseThrow().success());
        assertTrue(session.score().orElseThrow().science() > 0);
        assertEquals(3, session.score().orElseThrow().stars(), () -> session.score().toString());
        assertEquals(1, session.missionLog().entries().size());
        assertEquals(plan.mission(), session.missionLog().entries().getFirst().missionId());
    }

    /** The build's issues, as "KIND@SLOT", in the order the player reads them. */
    private static List<String> buildIssues(Plan plan) {
        GameSession session = play(plan);
        assertEquals(MissionStep.REVIEW, session.step(), "a build with issues must not get past Review");
        assertThrows(IllegalStateException.class, session::confirmBuild);
        assertFalse(session.isUnlocked(MissionStep.LAUNCH));
        return session.buildReport().issues().stream()
                .map(issue -> issue.kind() + "@" + issue.slot()).toList();
    }

    private static Report stoppedOnThePad(Plan plan, LaunchIssue.Kind first) {
        GameSession session = play(plan);
        assertEquals(MissionStep.LAUNCH, session.step(), () -> "build " + session.buildReport().issues());
        assertEquals(first, session.launchIssues().getFirst().kind(), () -> session.launchIssues().toString());
        assertFalse(session.isUnlocked(MissionStep.FLY));
        assertTrue(session.score().isEmpty());
        assertTrue(session.missionLog().entries().isEmpty());
        return session.report().orElseThrow();
    }

    // ---- Mission 1: Farm Watch --------------------------------------------------------------

    @Test
    void farmWatch_aSoundDesignFliesWithNothingBroken() {
        assertSucceeds(FARM);
    }

    @Test
    void farmWatch_modisIsTooCoarseAndTheReportSaysSoFirst() {
        GameSession session = play(FARM.withInstruments("modis"));
        assertFlown(session, ReportBuilder.BROKE_REQUIREMENT_TOO_COARSE);
        assertEquals(List.of("LAND_IMAGING", 30.0, 250.0), session.report().orElseThrow().brokeFirst().params());
        assertEquals(0, session.score().orElseThrow().science(), 1e-9);
    }

    @Test
    void farmWatch_noInstrumentMeansTheMeasurementIsMissing() {
        GameSession session = play(FARM.withInstruments());
        assertFlown(session, ReportBuilder.BROKE_REQUIREMENT_MISSING);
        assertEquals(ReportBuilder.TRY_ADD_INSTRUMENT_FOR, session.report().orElseThrow().tryNext().messageKey());
    }

    @Test
    void farmWatch_aRocketTooSmallStopsTheShipOnThePad() {
        Report report = stoppedOnThePad(FARM.withRocket("electron"), LaunchIssue.Kind.MASS_OVER_CAPACITY);
        assertEquals(ReportBuilder.BROKE_MASS_OVER_CAPACITY, report.brokeFirst().messageKey());
        // eo1-class 428.5 + mr-106 0.59 + clamshell 5 + bay 4.5 + CubeSat 6 + ALI 90 = 534.59 kg; Electron lifts 200 kg.
        assertEquals(534.59, (double) report.brokeFirst().params().get(0), 1e-9);
        assertEquals(200.0, (double) report.brokeFirst().params().get(1), 1e-9);
    }

    @Test
    void farmWatch_noRocketChosenStopsTheShipOnThePad() {
        Report report = stoppedOnThePad(FARM.withRocket(null), LaunchIssue.Kind.ROCKET_NOT_OFFERED);
        assertEquals(ReportBuilder.BROKE_ROCKET_NOT_OFFERED, report.brokeFirst().messageKey());
    }

    @Test
    void farmWatch_tooLittlePowerBlocksConfirmBuild() {
        // ALI draws 100 W; the 6U body panel gives 19.2 W.
        assertEquals(List.of("POWER_SHORT@POWER"),
                buildIssues(FARM.withParts("eo1-class", "mr-106", "sp6x-solar", "bay-6u", "cubesat-3u")));
    }

    @Test
    void farmWatch_aSatelliteTooBigForItsBayFailsEveryFitRule() {
        assertEquals(List.of("PAYLOAD_TOO_LONG@CARGO_BAY", "PAYLOAD_TOO_WIDE@CARGO_BAY", "PAYLOAD_TOO_HEAVY@CARGO_BAY"),
                buildIssues(FARM.withParts("eo1-class", "mr-106", "pumpkin-clamshell", "bay-3u", "cubesat-12u")));
    }

    @Test
    void farmWatch_theShuttleBlowsTheBudget() {
        GameSession session = design(FARM.withParts("shuttle-orbiter", "shuttle-oms", "shuttle-fuel-cell",
                "shuttle-bay", "cubesat-3u"));
        List<BuildIssue> issues = session.buildReport().issues();
        assertEquals(1, issues.size(), issues::toString);
        assertEquals(BuildIssue.Kind.OVER_BUDGET, issues.getFirst().kind());
        assertEquals(450, issues.getFirst().available(), 1e-9);
        assertEquals(session.missionResources().get(ResourceType.BUDGET).used(), issues.getFirst().needed(), 1e-6);
    }

    @Test
    void farmWatch_tooLongAScienceTimeMissesTheDeadline() {
        Report report = stoppedOnThePad(FARM.withScienceMonths(78), LaunchIssue.Kind.OVER_DEADLINE);
        assertEquals(ReportBuilder.BROKE_OVER_DEADLINE, report.brokeFirst().messageKey());
    }

    @Test
    void farmWatch_withNoGroundStationTheRecorderFillsAndNothingComesHome() {
        GameSession session = play(FARM.withStations());
        assertFlown(session, ReportBuilder.BROKE_STORAGE);
        assertEquals(ReportBuilder.TRY_ADD_STATION, session.report().orElseThrow().tryNext().messageKey());
        assertFalse(session.simulation().orElseThrow().success());
        assertEquals(0, session.simulation().orElseThrow().totalSentGbit());
        assertEquals(0, session.score().orElseThrow().science(), 1e-9);
    }

    @Test
    void farmWatch_redesignKeepsEveryPartAndChoice() {
        GameSession session = play(FARM.withInstruments("modis"));
        ShipBuild ship = session.build();
        MissionDesign design = session.design();

        session.redesign();

        assertEquals(MissionStep.SHIP, session.step());
        assertEquals(ship, session.build());
        assertTrue(session.build().isComplete());
        assertEquals(design, session.design());
        assertTrue(session.report().isEmpty());
        assertFalse(session.isUnlocked(MissionStep.LAUNCH));
    }

    // ---- Mission 2: Moon Landing ------------------------------------------------------------

    @Test
    void moonLanding_aSoundDesignFliesWithNothingBroken() {
        assertSucceeds(MOON);
    }

    @Test
    void moonLanding_fuelFollowsTheRocketEquationLegByLeg() {
        GameSession session = design(MOON);
        double isp = catalog.shipPart("rl10c-1").orElseThrow().specificImpulseS();
        double lander = catalog.shipPart("apollo-11-lander").orElseThrow().massKg();
        double carried = catalog.instrument("lola").orElseThrow().massKg() + 80;
        double surfaceKg = FuelMath.propellantKg(lander + carried, 4085, isp);
        double transitKg = FuelMath.propellantKg(session.buildReport().dryMassKg() + carried + surfaceKg, 1950, isp);
        assertEquals(surfaceKg + transitKg, session.buildReport().propellantKg(), 1e-6);
        // The textbook check: a speed change of Isp x g0 x ln 2 burns exactly the dry mass.
        assertEquals(1000, FuelMath.propellantKg(1000, isp * FuelMath.STANDARD_GRAVITY_MS2 * Math.log(2), isp), 1e-9);
    }

    @Test
    void moonLanding_tooFewCrewSuppliesBlocksConfirmBuild() {
        GameSession session = design(MOON.withSupplies(20));
        assertEquals(List.of(new BuildIssue(BuildIssue.Kind.SUPPLIES_SHORT, null, 8 * 2 * 4.99, 20)),
                session.buildReport().issues());
        assertThrows(IllegalStateException.class, session::confirmBuild);
    }

    @Test
    void moonLanding_aWeakThrusterRunsTheLanderOutOfFuelFirst() {
        List<String> issues = buildIssues(MOON.withParts("apollo-service-module", "mr-103", "shuttle-fuel-cell",
                "apollo-11-adapter", "apollo-11-lander"));
        assertEquals("FUEL_SHORT@SATELLITE", issues.getFirst(), issues::toString);
    }

    @Test
    void moonLanding_theCrewCabinIsTooHeavyForTheApolloServiceModule() {
        List<String> issues = buildIssues(MOON.withParts("apollo-service-module", "rl10c-1", "shuttle-fuel-cell",
                "apollo-11-adapter", "orion-cabin"));
        assertEquals("PAYLOAD_TOO_HEAVY@AIRFRAME", issues.getFirst(), issues::toString);
    }

    @Test
    void moonLanding_aRocketThatDoesNotFlyToTheMoonIsNotOffered() {
        Report report = stoppedOnThePad(MOON.withRocket("falcon-9"), LaunchIssue.Kind.ROCKET_NOT_OFFERED);
        assertEquals(ReportBuilder.BROKE_ROCKET_NOT_OFFERED, report.brokeFirst().messageKey());
    }

    @Test
    void moonLanding_withoutTheAltimeterTheTerrainIsNotMeasured() {
        GameSession session = play(MOON.withInstruments("diviner"));
        assertFlown(session, ReportBuilder.BROKE_REQUIREMENT_MISSING);
        assertEquals(List.of("LUNAR_TOPOGRAPHY"), session.report().orElseThrow().brokeFirst().params());
    }

    @Test
    void moonLanding_withNoGroundStationTheDataWaitsOnBoard() {
        GameSession session = play(MOON.withStations());
        assertFlown(session, ReportBuilder.BROKE_DOWNLINK);
        assertEquals(ReportBuilder.TRY_ADD_STATION, session.report().orElseThrow().tryNext().messageKey());
    }

    // ---- Mission 3: Mars Sample Drones ------------------------------------------------------

    @Test
    void marsDrones_aSoundDesignFliesWithNothingBroken() {
        assertSucceeds(MARS);
    }

    @Test
    void marsDrones_aSmallSolarPanelCannotRunTheInstruments() {
        assertEquals(List.of("POWER_SHORT@POWER"),
                buildIssues(MARS.withParts("curiosity-cruise", "mr-106", "sp6x-solar", "mars-2020-entry", "perseverance")));
    }

    @Test
    void marsDrones_theBigRoverDoesNotFitASmallAeroshell() {
        List<String> issues = buildIssues(MARS.withParts("curiosity-cruise", "mr-106", "mmrtg",
                "insight-aeroshell", "perseverance"));
        assertEquals(List.of("PAYLOAD_TOO_LONG@CARGO_BAY", "PAYLOAD_TOO_WIDE@CARGO_BAY",
                "PAYLOAD_TOO_HEAVY@CARGO_BAY"), issues.subList(0, 3), issues::toString);
    }

    @Test
    void marsDrones_anEntryStageWithoutDescentTanksCannotLand() {
        List<String> issues = buildIssues(MARS.withParts("spirit-cruise", "mr-106", "mmrtg",
                "spirit-aeroshell", "spirit"));
        assertEquals(List.of("FUEL_SHORT@CARGO_BAY"), issues);
    }

    @Test
    void marsDrones_aCruiseStageTooSmallForTheRoverCannotCarryIt() {
        List<String> issues = buildIssues(MARS.withParts("insight-cruise", "mr-106", "mmrtg",
                "mars-2020-entry", "perseverance"));
        assertEquals("PAYLOAD_TOO_HEAVY@AIRFRAME", issues.getFirst(), issues::toString);
    }

    @Test
    void marsDrones_aRocketThatDoesNotFlyToMarsIsNotOffered() {
        Report report = stoppedOnThePad(MARS.withRocket("electron"), LaunchIssue.Kind.ROCKET_NOT_OFFERED);
        assertEquals(ReportBuilder.BROKE_ROCKET_NOT_OFFERED, report.brokeFirst().messageKey());
    }

    @Test
    void marsDrones_aCameraAloneCannotSayWhatTheRocksAreMadeOf() {
        GameSession session = play(MARS.withInstruments("mastcam-z"));
        assertFlown(session, ReportBuilder.BROKE_REQUIREMENT_MISSING);
        assertEquals(List.of("MARS_SURFACE_COMPOSITION"), session.report().orElseThrow().brokeFirst().params());
    }

    // ---- optional Earth missions --------------------------------------------------------------

    @Test
    void stormChasers_aSoundDesignFliesWithNothingBroken() {
        assertSucceeds(STORM);
    }

    @Test
    void stormChasers_aPolarStationNeverSeesTheTropicalOrbit() {
        GameSession session = play(STORM.withStations("svalbard"));
        assertFlown(session, ReportBuilder.BROKE_DOWNLINK);
        assertEquals(ReportBuilder.TRY_ADD_STATION_OR_FASTER_RADIO,
                session.report().orElseThrow().tryNext().messageKey());
        assertEquals(0, session.simulation().orElseThrow().totalSentGbit());
    }

    @Test
    void stormChasers_theSmallestRocketCannotLiftIt() {
        Report report = stoppedOnThePad(STORM.withRocket("electron"), LaunchIssue.Kind.MASS_OVER_CAPACITY);
        assertEquals(ReportBuilder.BROKE_MASS_OVER_CAPACITY, report.brokeFirst().messageKey());
    }

    @Test
    void stormChasers_aLightningSensorDoesNotMeasureRain() {
        GameSession session = play(STORM.withInstruments("lis"));
        assertFlown(session, ReportBuilder.BROKE_REQUIREMENT_MISSING);
        assertEquals(List.of("RAIN_MICROWAVE"), session.report().orElseThrow().brokeFirst().params());
    }

    @Test
    void icePatrol_aSoundDesignFliesWithNothingBroken() {
        assertSucceeds(ICE);
    }

    @Test
    void icePatrol_theSunSynchronousOrbitNeverReachesTheIce() {
        Report report = stoppedOnThePad(ICE.withOrbit("sso-705"), LaunchIssue.Kind.ORBIT_CANNOT_REACH_TARGET);
        assertEquals(ReportBuilder.BROKE_ORBIT_CANNOT_REACH_TARGET, report.brokeFirst().messageKey());
        assertEquals(86.0, (double) report.brokeFirst().params().get(0), 1e-9);
        assertEquals(81.8, (double) report.brokeFirst().params().get(1), 1e-9);
    }

    @Test
    void icePatrol_falcon9sPolarFigureIsTooSmall() {
        Report report = stoppedOnThePad(ICE.withRocket("falcon-9"), LaunchIssue.Kind.MASS_OVER_CAPACITY);
        assertEquals(ReportBuilder.BROKE_MASS_OVER_CAPACITY, report.brokeFirst().messageKey());
    }

    @Test
    void icePatrol_aSecondBigInstrumentBlowsTheBudget() {
        List<String> issues = buildIssues(ICE.withInstruments("glas", "amsr-e"));
        assertEquals(List.of("OVER_BUDGET@null"), issues);
    }

    // ---- every mission --------------------------------------------------------------------

    @Test
    void anEmptyShipNeverReachesSupplies() {
        for (var mission : catalog.missions()) {
            GameSession session = new GameSession(catalog);
            session.selectMission(mission.id());
            session.next();
            assertEquals(MissionFlow.SHIP_INCOMPLETE,
                    assertThrows(IllegalStateException.class, session::next).getMessage());
            assertEquals(MissionStep.SHIP, session.step());
            assertEquals(ModuleSlot.values().length, session.buildReport().issues().stream()
                    .filter(issue -> issue.kind() == BuildIssue.Kind.SLOT_EMPTY).count());
        }
    }
}
