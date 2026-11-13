package com.liftoff;

import com.liftoff.model.Bus;
import com.liftoff.model.GroundStation;
import com.liftoff.model.Instrument;
import com.liftoff.model.LaunchClass;
import com.liftoff.model.LaunchVehicle;
import com.liftoff.model.Objective;
import com.liftoff.model.Orbit;
import com.liftoff.model.SourceRef;
import com.liftoff.model.ValueBasis;

import java.util.List;
import java.util.Map;

/** Simple hand-made parts with round numbers, so test arithmetic can be checked by eye. */
public final class TestParts {

    public static final SourceRef SOURCE = new SourceRef("Test source", "made up for tests", "https://example.test");

    private TestParts() {
    }

    public static Instrument instrument(String id, double massKg, double powerW, double dataKbps,
                                        double costMusd, int buildMonths) {
        return new Instrument(id, id, id, "", "", massKg, powerW, ValueBasis.PUBLISHED, dataKbps,
                ValueBasis.PUBLISHED, List.of(), costMusd, ValueBasis.GAME_ESTIMATE, buildMonths,
                ValueBasis.GAME_ESTIMATE, SOURCE);
    }

    public static Bus bus(String id, double massKg, double payloadPowerW, double downlinkMbps,
                          double costMusd, int buildMonths) {
        return new Bus(id, id, id, "", "", massKg, ValueBasis.DERIVED, 100, payloadPowerW,
                ValueBasis.DERIVED, downlinkMbps, 60, costMusd, ValueBasis.GAME_ESTIMATE, buildMonths,
                ValueBasis.GAME_ESTIMATE, SOURCE);
    }

    public static Orbit orbit(String id, double altitudeKm, double inclinationDeg, LaunchClass launchClass) {
        return new Orbit(id, id, id, "", "", altitudeKm, inclinationDeg, false, launchClass, SOURCE);
    }

    public static GroundStation station(String id, double latitudeDeg, double costMusdPerMonth) {
        return new GroundStation(id, id, id, latitudeDeg, 0, "S/X", costMusdPerMonth,
                ValueBasis.GAME_ESTIMATE, SOURCE);
    }

    public static LaunchVehicle rocket(String id, Map<LaunchClass, Double> capacityKg, double costMusd) {
        return new LaunchVehicle(id, id, id, "", capacityKg, costMusd, ValueBasis.PUBLISHED, SOURCE);
    }

    public static Objective objective(String id, double budgetCapMusd, int deadlineMonths, int minScienceMonths) {
        return new Objective(id, id, "", "", "", 0, false, List.of(), List.of(), budgetCapMusd,
                deadlineMonths, minScienceMonths, SOURCE);
    }
}
