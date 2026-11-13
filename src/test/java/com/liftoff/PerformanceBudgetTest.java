package com.liftoff;

import com.liftoff.data.CatalogLoader;
import com.liftoff.data.MissionEntry;
import com.liftoff.engine.BuildCalculator;
import com.liftoff.engine.BuildLoad;
import com.liftoff.model.Catalog;
import com.liftoff.model.ModuleSlot;
import com.liftoff.model.ShipBuild;
import com.liftoff.render3d.ModelLibrary;
import com.liftoff.session.GameSession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Time budgets that keep the game feeling instant. The limits are many times what this code needs
 * on a slow laptop, so they never fail by chance; they fail when a change makes something an order
 * of magnitude slower. A frame lasts about 16 ms; anything the player triggers must fit in one.
 */
class PerformanceBudgetTest {

    private static Catalog catalog;

    @BeforeAll
    static void load() {
        catalog = CatalogLoader.loadFromClasspath();
    }

    private static double millis(Runnable work) {
        work.run();   // warm up the JIT and the caches, as a player's first minute does
        long start = System.nanoTime();
        work.run();
        return (System.nanoTime() - start) / 1e6;
    }

    private static void within(double budgetMs, double tookMs, String what) {
        assertTrue(tookMs <= budgetMs, what + " took " + tookMs + " ms; the budget is " + budgetMs + " ms");
    }

    @Test
    void aThousandRandomBuildsAreWorkedOutWellInsideOneFrameEach() {
        SplittableRandom random = new SplittableRandom(1);
        List<MissionEntry> missions = catalog.missions();
        double took = millis(() -> {
            for (int i = 0; i < 1000; i++) {
                MissionEntry entry = missions.get(random.nextInt(missions.size()));
                ShipBuild build = ShipBuild.empty();
                for (ModuleSlot slot : ModuleSlot.values()) {
                    var parts = catalog.partsFor(entry.id(), slot);
                    build = build.with(parts.get(random.nextInt(parts.size())));
                }
                BuildCalculator.calculate(build, entry.toMission(),
                        new BuildLoad(random.nextDouble() * 100, random.nextDouble() * 100, 0, 0, 0));
            }
        });
        within(500, took, "1,000 builds");
    }

    @Test
    void everyLadderMissionFliesAHundredTimesInWellUnderASecond() {
        for (String[] plan : new String[][] {
                {"farm-watch", "eo1-class,mr-106,pumpkin-clamshell,bay-6u,cubesat-3u", "ali", "atlas-v-401"},
                {"moon-landing", "apollo-service-module,rl10c-1,shuttle-fuel-cell,apollo-11-adapter,apollo-11-lander",
                        "lola", "saturn-v"},
                {"mars-drones", "curiosity-cruise,mr-106,mmrtg,mars-2020-entry,perseverance",
                        "mastcam-z,supercam", "atlas-v-541"}}) {
            double took = millis(() -> {
                for (long seed = 0; seed < 100; seed++) {
                    readyToFly(plan).fly(seed);
                }
            });
            within(1000, took, "100 flights of " + plan[0]);
        }
    }

    @Test
    void theGaugesCanBeRecomputedAThousandTimesWithinASecond() {
        GameSession session = readyToFly(new String[] {"farm-watch",
                "eo1-class,mr-106,pumpkin-clamshell,bay-6u,cubesat-3u", "ali", "atlas-v-401"});
        double took = millis(() -> {
            for (int i = 0; i < 1000; i++) {
                session.missionResources();
                session.buildReport();
            }
        });
        within(1000, took, "1,000 gauge refreshes");
    }

    @Test
    void aPartSwapNeedsNoFileWorkOnceModelsArePreloaded() throws Exception {
        List<String> ids = catalog.shipParts().stream().map(part -> part.modelId()).distinct().toList();
        ModelLibrary.preload(ids).get(30, TimeUnit.SECONDS);
        FxTest.run(() -> {
            var library = new ModelLibrary();
            double took = millis(() -> {
                for (int i = 0; i < 20; i++) {
                    ids.forEach(library::create);
                }
            });
            within(1000, took, "20 x " + ids.size() + " model copies");
        });
    }

    @Test
    void theCatalogLoadsWithinASecondOnceTheJvmIsWarm() {
        within(1000, millis(CatalogLoader::loadFromClasspath), "catalog load");
    }

    private static GameSession readyToFly(String[] plan) {
        GameSession session = new GameSession(catalog);
        session.selectMission(plan[0]);
        session.next();
        for (String part : plan[1].split(",")) {
            session.choosePart(part);
        }
        session.next();
        for (String instrument : plan[2].split(",")) {
            session.addInstrument(instrument);
        }
        if (plan[0].equals("farm-watch")) {
            session.chooseOrbit("sso-705");
        } else if (plan[0].equals("moon-landing")) {
            session.setCrewSuppliesKg(80);
        }
        session.toggleStation("svalbard");
        session.chooseLaunchVehicle(plan[3]);
        session.next();
        session.confirmBuild();
        session.next();
        session.runLaunchCheck();
        session.next();
        return session;
    }
}
