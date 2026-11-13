package com.liftoff.session;

import com.liftoff.engine.BuildReport;
import com.liftoff.engine.ResourceReport;
import com.liftoff.engine.score.Report;
import com.liftoff.engine.score.ReportBuilder;
import com.liftoff.engine.score.Score;
import com.liftoff.engine.score.ScoreCalculator;
import com.liftoff.engine.sim.EventDeck;
import com.liftoff.engine.sim.MissionSimulator;
import com.liftoff.engine.sim.SimExtras;
import com.liftoff.engine.sim.SimulationResult;
import com.liftoff.model.Difficulty;
import com.liftoff.model.Mission;
import com.liftoff.model.MissionDesign;

import java.util.List;

/**
 * A flight with every input fixed, made by {@link GameSession#prepareFlight}. Everything in it is
 * immutable, so {@link #compute} may run on any thread; the result goes back through
 * {@link GameSession#land}.
 *
 * @param deck   the events to draw from; null for a design without a modular ship, which uses the
 *               bundled deck and the bus's own radio
 * @param link   the ship's power and radio; null under the same condition as {@code deck}
 */
public record Flight(GameSession owner, long preparedAt, MissionDesign design, Mission mission, Difficulty difficulty,
                     long seed, BuildReport build, ResourceReport resources, EventDeck deck, SimExtras extras,
                     MissionSimulator.DeepSpaceLink link) {

    /** What a computed flight brings back. */
    public record Outcome(Flight flight, SimulationResult simulation, Score score, Report report) {
    }

    boolean modular() {
        return deck != null;
    }

    /** Simulates, scores and explains the flight. Pure: no session state is read or changed. */
    public Outcome compute() {
        SimulationResult simulation = modular()
                ? MissionSimulator.run(design, mission, difficulty, seed, deck, extras, link)
                : MissionSimulator.run(design, mission, difficulty, seed);
        Score score = modular()
                ? ScoreCalculator.scoreCombined(design, resources, build, simulation, mission)
                : ScoreCalculator.score(design, resources, build, simulation, mission);
        Report report = ReportBuilder.build(design, resources, List.of(), simulation, mission);
        return new Outcome(this, simulation, score, report);
    }
}
