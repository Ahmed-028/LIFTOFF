package com.liftoff.engine.sim;

import com.liftoff.TestParts;
import com.liftoff.model.LaunchClass;
import com.liftoff.model.MissionDesign;
import com.liftoff.model.Objective;
import com.liftoff.session.stub.Mission;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;


public final class SimFixtures {

    public static final Mission EARTH = new Mission("test-earth", 0);

    private SimFixtures() {
    }

    public static Objective objective(double targetLatitudeDeg, double budgetCapMusd, int deadlineMonths) {
        return new Objective("goal", "goal", "", "", "", targetLatitudeDeg, false, List.of(), List.of(),
                budgetCapMusd, deadlineMonths, 12, TestParts.SOURCE);
    }


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
