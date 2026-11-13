package com.liftoff.engine.sim;

import com.liftoff.TestParts;
import com.liftoff.model.Destination;
import com.liftoff.model.Difficulty;
import com.liftoff.model.LaunchClass;
import com.liftoff.model.Mission;
import com.liftoff.model.MissionDesign;
import com.liftoff.model.Objective;
import com.liftoff.model.StepUnit;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Round-number designs and the event fixture for the simulation tests. Nothing here is real data. */
public final class SimFixtures {

    public static final Mission EARTH = SimFixtures.mission("test-earth", 0);

    private SimFixtures() {
    }

    /** A robotic Earth mission stepped in months for as long as the design's science duration. */
    public static Mission mission(String id, double deltaVRequiredMs) {
        return mission(id, Destination.EARTH_ORBIT, StepUnit.MONTH, 0, deltaVRequiredMs, 0, 0);
    }

    public static Mission mission(String id, Destination destination, StepUnit stepUnit, int steps,
                                  double deltaVRequiredMs, int crew, double suppliesKgPerCrewDay) {
        return new Mission(id, id, "", "", "", Difficulty.EASY, destination, true, 0, false,
                List.of(), List.of(), 1_000_000, 1000, 0, deltaVRequiredMs, suppliesKgPerCrewDay,
                stepUnit, steps, crew, 0, TestParts.SOURCE);
    }

    public static Objective objective(double targetLatitudeDeg, double budgetCapMusd, int deadlineMonths) {
        return new Objective("goal", "goal", "", "", "", targetLatitudeDeg, false, List.of(), List.of(),
                budgetCapMusd, deadlineMonths, 12, TestParts.SOURCE);
    }

    /**
     * 150 kg (bus 100 + instrument 50), 50 W drawn of 100 W, 100 kbps, polar orbit reaching 82 degrees,
     * one polar station, a rocket lifting 1000 kg to polar orbit, 12 science months.
     * Budget 10 + 5 + 20 + 1 x 12 = 47; time 12 + 6 + 2 + 12 = 32 months.
     */
    public static MissionDesign design() {
        return MissionDesign.empty()
                .withObjective(objective(80, 1000, 200))
                .withBus(TestParts.bus("bus", 100, 100, 10, 10, 12))
                .plusInstrument(TestParts.instrument("camera", 50, 50, 100, 5, 12))
                .withOrbit(TestParts.orbit("polar", 700, 98, LaunchClass.POLAR))
                .plusStation(TestParts.station("north", 78, 1))
                .withLaunchVehicle(TestParts.rocket("rocket", Map.of(LaunchClass.POLAR, 1000.0), 20))
                .withScienceMonths(12);
    }

    public static EventDeck deck() {
        try (InputStream in = SimFixtures.class.getResourceAsStream("/com/liftoff/sim/events-fixture.json")) {
            return EventDeck.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
