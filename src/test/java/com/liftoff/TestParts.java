package com.liftoff;

import com.liftoff.model.Bus;
import com.liftoff.model.Capability;
import com.liftoff.model.Destination;
import com.liftoff.model.Difficulty;
import com.liftoff.model.GroundStation;
import com.liftoff.model.Instrument;
import com.liftoff.model.LaunchClass;
import com.liftoff.model.LaunchVehicle;
import com.liftoff.model.Mission;
import com.liftoff.model.ModuleSlot;
import com.liftoff.model.Objective;
import com.liftoff.model.Orbit;
import com.liftoff.model.Requirement;
import com.liftoff.model.ShipPart;
import com.liftoff.model.SourceRef;
import com.liftoff.model.ValueBasis;

import java.util.List;
import java.util.Map;


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


    public static Instrument instrument(String id, List<Capability> capabilities) {
        return new Instrument(id, id, id, "", "", 10, 10, ValueBasis.PUBLISHED, 0,
                ValueBasis.PUBLISHED, capabilities, 0, ValueBasis.GAME_ESTIMATE, 0,
                ValueBasis.GAME_ESTIMATE, SOURCE);
    }


    public static Mission mission(String id, double budgetCapMusd, double deltaVRequiredMs) {
        return mission(id, budgetCapMusd, deltaVRequiredMs, 0, List.of());
    }


    public static Mission mission(String id, double budgetCapMusd, double deltaVRequiredMs,
                                  double targetLatitudeDeg, List<Requirement> required) {
        return new Mission(id, id, "", "", "", Difficulty.EASY, Destination.EARTH_ORBIT, true,
                targetLatitudeDeg, false, required, List.of(), budgetCapMusd, 36, 12,
                deltaVRequiredMs, 0, SOURCE);
    }


    public static ShipPart airframe(String id, double massKg, double payloadKg, double tanksKg,
                                    double powerDrawW, double costMusd) {
        return shipPart(id, ModuleSlot.AIRFRAME, massKg, 0, powerDrawW, payloadKg, 0, 0, 0, 0, 0, tanksKg, costMusd);
    }

    public static ShipPart thruster(String id, double massKg, double specificImpulseS,
                                    double powerDrawW, double costMusd) {
        return shipPart(id, ModuleSlot.THRUSTERS, massKg, 0, powerDrawW, 0, 0, 0, 0, 0, specificImpulseS, 0, costMusd);
    }

    public static ShipPart power(String id, double massKg, double powerSupplyW, double costMusd) {
        return shipPart(id, ModuleSlot.POWER, massKg, powerSupplyW, 0, 0, 0, 0, 0, 0, 0, 0, costMusd);
    }


    public static ShipPart cargoBay(String id, double massKg, double payloadKg, double lengthM,
                                    double diameterM, double costMusd) {
        return shipPart(id, ModuleSlot.CARGO_BAY, massKg, 0, 0, payloadKg, lengthM, diameterM, 0, 0, 0, 0, costMusd);
    }


    public static ShipPart satellite(String id, double massKg, double lengthM, double diameterM,
                                     double powerDrawW, double costMusd) {
        return shipPart(id, ModuleSlot.SATELLITE, massKg, 0, powerDrawW, 0, 0, 0, lengthM, diameterM, 0, 0, costMusd);
    }

    private static ShipPart shipPart(String id, ModuleSlot slot, double massKg, double powerSupplyW,
                                     double powerDrawW, double payloadKg, double bayLengthM, double bayDiameterM,
                                     double lengthM, double diameterM, double specificImpulseS, double tanksKg,
                                     double costMusd) {
        return new ShipPart(id, slot, id, id, "", "", List.of(),
                massKg, powerSupplyW, powerDrawW,
                payloadKg, bayLengthM, bayDiameterM,
                lengthM, diameterM,
                specificImpulseS > 0 ? 1000 : 0, specificImpulseS, tanksKg,
                0, costMusd, ValueBasis.GAME_ESTIMATE,
                "model-" + id, SOURCE);
    }
}
