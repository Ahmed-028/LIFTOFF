package com.liftoff;

import com.liftoff.data.CatalogLoader;
import com.liftoff.data.MissionEntry;
import com.liftoff.engine.BuildCalculator;
import com.liftoff.engine.BuildLoad;
import com.liftoff.engine.OrbitMath;
import com.liftoff.engine.ResourceCalculator;
import com.liftoff.engine.score.ReportBuilder;
import com.liftoff.engine.score.ScoreCalculator;
import com.liftoff.engine.sim.EventDeck;
import com.liftoff.engine.sim.LaunchChecker;
import com.liftoff.engine.sim.MissionSimulator;
import com.liftoff.engine.sim.SimExtras;
import com.liftoff.model.Catalog;
import com.liftoff.model.Difficulty;
import com.liftoff.model.GroundStation;
import com.liftoff.model.Instrument;
import com.liftoff.model.MissionDesign;
import com.liftoff.model.ModuleSlot;
import com.liftoff.model.Orbit;
import com.liftoff.model.ShipBuild;
import com.liftoff.model.ShipPart;
import com.liftoff.session.GameSession;
import com.liftoff.session.MissionStep;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Golden values: every engine output for a fixed set of designs, written out in full precision
 * and compared with the copy in src/test/resources/com/liftoff/golden. Any refactor that changes a
 * number, even in the last bit, fails here. The designs are drawn with fixed seeds, so the run is
 * the same on every machine.
 *
 * <p>To accept a deliberate change of behaviour, run with {@code -Dgolden.update=true} and review
 * the diff of the golden files like any other change.
 */
class GoldenEngineTest {

    private static final Path GOLDEN = Path.of("src", "test", "resources", "com", "liftoff", "golden");

    private static Catalog catalog;

    @BeforeAll
    static void load() {
        catalog = CatalogLoader.loadFromClasspath();
    }

    // ---- the engine, called directly ------------------------------------------------------

    @Test
    void buildCalculatorOnRandomBuildsForEveryMission() throws IOException {
        StringBuilder out = new StringBuilder();
        SplittableRandom random = new SplittableRandom(20261113);
        for (MissionEntry entry : catalog.missions()) {
            var mission = entry.toMission();
            for (int i = 0; i < 60; i++) {
                ShipBuild build = randomBuild(random, entry.id(), random.nextInt(4) == 0);
                BuildLoad load = new BuildLoad(random.nextDouble() * 500, random.nextDouble() * 300,
                        random.nextDouble() * 200, random.nextDouble() * 120, random.nextDouble() * 400);
                out.append(entry.id()).append(' ').append(ids(build)).append('\n')
                        .append("  bare ").append(BuildCalculator.calculate(build, mission)).append('\n')
                        .append("  load ").append(BuildCalculator.calculate(build, mission, load)).append('\n')
                        .append("  none ").append(BuildCalculator.calculate(build, null)).append('\n');
            }
        }
        compare("build-calculator.txt", out);
    }

    @Test
    void resourceCalculatorAndLaunchCheckerOnRandomDesigns() throws IOException {
        StringBuilder out = new StringBuilder();
        SplittableRandom random = new SplittableRandom(42);
        for (MissionEntry entry : catalog.missions()) {
            var mission = entry.toMission();
            for (int i = 0; i < 40; i++) {
                MissionDesign design = randomDesign(random, entry, i % 2 == 0);
                var build = BuildCalculator.calculate(design.build(), mission);
                out.append(entry.id()).append(' ').append(describe(design)).append('\n')
                        .append("  resources ").append(ResourceCalculator.calculate(design)).append('\n')
                        .append("  launch ").append(LaunchChecker.check(design, build, mission,
                                design.launchVehicle(), design.orbit())).append('\n')
                        .append("  launch-v1 ").append(LaunchChecker.check(design, mission,
                                design.launchVehicle(), design.orbit())).append('\n');
            }
        }
        compare("resources-and-launch.txt", out);
    }

    @Test
    void orbitMathForEveryOrbitAndStation() throws IOException {
        StringBuilder out = new StringBuilder();
        for (Orbit orbit : catalog.orbits()) {
            out.append(orbit.id()).append(" period ").append(OrbitMath.periodMinutes(orbit.altitudeKm()))
                    .append(" reach ").append(OrbitMath.maxLatitudeDeg(orbit.inclinationDeg()))
                    .append(" view ").append(OrbitMath.viewAngleDeg(orbit.altitudeKm(), 5)).append('\n');
            for (GroundStation station : catalog.groundStations()) {
                out.append("  ").append(station.id()).append(' ')
                        .append(OrbitMath.contactMinutesPerDay(orbit, station.latitudeDeg())).append('\n');
            }
            for (double latitude = -90; latitude <= 90; latitude += 7.5) {
                out.append("  lat ").append(latitude).append(' ')
                        .append(OrbitMath.contactMinutesPerDay(orbit, latitude)).append('\n');
            }
        }
        compare("orbit-math.txt", out);
    }

    @Test
    void simulatorAndScoreOnRandomDesignsAtEveryDifficulty() throws IOException {
        StringBuilder out = new StringBuilder();
        SplittableRandom random = new SplittableRandom(7);
        EventDeck deck = new EventDeck(catalog.events());
        for (MissionEntry entry : catalog.missions()) {
            var mission = entry.toMission();
            for (int i = 0; i < 12; i++) {
                MissionDesign design = randomDesign(random, entry, true);
                var build = BuildCalculator.calculate(design.build(), mission);
                var resources = ResourceCalculator.calculate(design);
                for (Difficulty difficulty : Difficulty.values()) {
                    long seed = random.nextLong();
                    var link = new MissionSimulator.DeepSpaceLink(random.nextDouble() * 400,
                            random.nextDouble() * 120, random.nextDouble() * 60);
                    var extras = new SimExtras(random.nextBoolean() ? Double.POSITIVE_INFINITY : random.nextDouble() * 100,
                            random.nextDouble() * 200, random.nextBoolean());
                    var plain = MissionSimulator.run(design, mission, difficulty, seed);
                    var linked = MissionSimulator.run(design, mission, difficulty, seed, deck, extras, link);
                    out.append(entry.id()).append(' ').append(difficulty).append(' ').append(seed).append(' ')
                            .append(describe(design)).append('\n')
                            .append("  sim ").append(plain).append('\n')
                            .append("  sim-link ").append(linked).append('\n')
                            .append("  score ").append(ScoreCalculator.score(design, resources, build, plain, mission))
                            .append('\n')
                            .append("  score-combined ")
                            .append(ScoreCalculator.scoreCombined(design, resources, build, linked, mission))
                            .append('\n')
                            .append("  report ").append(ReportBuilder.build(design, resources, List.of(), plain, mission))
                            .append('\n');
                }
            }
        }
        compare("simulator-and-score.txt", out);
    }

    // ---- the same through the session, the way the screens drive it -----------------------

    @Test
    void wholeMissionsThroughTheSession() throws IOException {
        StringBuilder out = new StringBuilder();
        SplittableRandom random = new SplittableRandom(99);
        for (MissionEntry entry : catalog.missions()) {
            for (int i = 0; i < 25; i++) {
                GameSession session = new GameSession(catalog);
                session.selectMission(entry.id());
                session.next();
                ShipBuild build = randomBuild(random, entry.id(), false);
                build.parts().values().forEach(part -> session.choosePart(part.id()));
                out.append(entry.id()).append(' ').append(ids(build)).append('\n');
                session.next();
                randomInstruments(random, entry).forEach(instrument -> session.addInstrument(instrument.id()));
                if (entry.destination() == com.liftoff.model.Destination.EARTH_ORBIT) {
                    session.chooseOrbit(pick(random, catalog.orbits()).id());
                    session.setScienceMonths(entry.briefing().minScienceMonths() + random.nextInt(24));
                }
                for (int s = random.nextInt(3); s >= 0; s--) {
                    session.toggleStation(pick(random, catalog.groundStations()).id());
                }
                session.chooseLaunchVehicle(pick(random, catalog.launchVehicles()).id());
                if (entry.crew() > 0) {
                    session.setCrewSuppliesKg(Math.floor(random.nextDouble() * 120));
                }
                session.setDifficulty(Difficulty.values()[random.nextInt(3)]);
                out.append("  design ").append(describe(session.design())).append('\n')
                        .append("  build ").append(session.buildReport()).append('\n')
                        .append("  resources ").append(session.resources()).append('\n')
                        .append("  mission-resources ").append(session.missionResources()).append('\n')
                        .append("  design-issues ").append(session.designIssues()).append('\n')
                        .append("  supplies-needed ").append(session.requiredCrewSuppliesKg()).append('\n');
                session.next();
                if (!session.buildReport().canConfirm()) {
                    continue;
                }
                session.confirmBuild();
                session.next();
                out.append("  launch ").append(session.runLaunchCheck()).append('\n');
                if (!session.isUnlocked(MissionStep.FLY)) {
                    out.append("  report ").append(session.report().orElseThrow()).append('\n');
                    continue;
                }
                session.next();
                out.append("  flight ").append(session.fly(random.nextLong())).append('\n')
                        .append("  score ").append(session.score().orElseThrow()).append('\n')
                        .append("  report ").append(session.report().orElseThrow()).append('\n');
            }
        }
        compare("whole-missions.txt", out);
    }

    // ---- random designs ---------------------------------------------------------------------

    private static ShipBuild randomBuild(SplittableRandom random, String missionId, boolean leaveGaps) {
        ShipBuild build = ShipBuild.empty();
        for (ModuleSlot slot : ModuleSlot.values()) {
            if (leaveGaps && random.nextInt(3) == 0) {
                continue;
            }
            build = build.with(pick(random, catalog.partsFor(missionId, slot)));
        }
        return build;
    }

    private static List<Instrument> randomInstruments(SplittableRandom random, MissionEntry entry) {
        return catalog.instruments().stream().filter(instrument -> random.nextInt(4) == 0).toList();
    }

    private static MissionDesign randomDesign(SplittableRandom random, MissionEntry entry, boolean asMission) {
        MissionDesign design = MissionDesign.empty().withObjective(entry.briefing())
                .withBuild(randomBuild(random, entry.id(), !asMission))
                .withScienceMonths(random.nextInt(40));
        if (asMission) {
            design = design.withMission(entry.toMission());
        } else if (random.nextBoolean()) {
            design = design.withBus(pick(random, catalog.buses()));
        }
        for (Instrument instrument : randomInstruments(random, entry)) {
            design = design.plusInstrument(instrument);
        }
        for (int s = random.nextInt(4); s > 0; s--) {
            design = design.plusStation(pick(random, catalog.groundStations()));
        }
        if (random.nextInt(5) > 0) {
            design = design.withOrbit(pick(random, catalog.orbits()));
        }
        if (random.nextInt(5) > 0) {
            design = design.withLaunchVehicle(pick(random, catalog.launchVehicles()));
        }
        return design;
    }

    private static <T> T pick(SplittableRandom random, List<T> options) {
        return options.get(random.nextInt(options.size()));
    }

    private static String ids(ShipBuild build) {
        return build.parts().values().stream().map(ShipPart::id).toList().toString();
    }

    private static String describe(MissionDesign design) {
        return "build=" + ids(design.build())
                + " bus=" + (design.bus() == null ? null : design.bus().id())
                + " mission=" + (design.mission() == null ? null : design.mission().id())
                + " instruments=" + design.instruments().stream().map(Instrument::id).toList()
                + " orbit=" + (design.orbit() == null ? null : design.orbit().id())
                + " stations=" + design.stations().stream().map(GroundStation::id).toList()
                + " rocket=" + (design.launchVehicle() == null ? null : design.launchVehicle().id())
                + " months=" + design.scienceMonths();
    }

    // ---- comparing with the golden copy -----------------------------------------------------

    private static void compare(String name, StringBuilder actual) throws IOException {
        String text = actual.toString();
        if (Boolean.getBoolean("golden.update")) {
            Files.createDirectories(GOLDEN);
            Files.writeString(GOLDEN.resolve(name), text, StandardCharsets.UTF_8);
            return;
        }
        try (InputStream in = GoldenEngineTest.class.getResourceAsStream("/com/liftoff/golden/" + name)) {
            assertNotNull(in, "no golden copy of " + name + "; run once with -Dgolden.update=true");
            String expected = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            String[] expectedLines = expected.split("\n", -1);
            String[] actualLines = text.split("\n", -1);
            for (int i = 0; i < Math.min(expectedLines.length, actualLines.length); i++) {
                assertEquals(expectedLines[i], actualLines[i], name + " line " + (i + 1));
            }
            assertEquals(expectedLines.length, actualLines.length, name + " line count");
        }
    }
}
