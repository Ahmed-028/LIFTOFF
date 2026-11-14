package com.liftoff;

import com.liftoff.model.Destination;
import com.liftoff.model.Difficulty;
import com.liftoff.model.Mission;
import com.liftoff.model.ModuleSlot;
import com.liftoff.model.ShipBuild;
import com.liftoff.model.ShipPart;
import com.liftoff.model.SourceRef;
import com.liftoff.model.ValueBasis;

import java.util.List;

// Synthetic test fixtures; never present these numbers as NASA data.
public final class SampleData {

    public static final SourceRef SOURCE =
            new SourceRef("Sample data - not real", "Made up for development and tests", "https://example.test");

    public static final String MISSION_ID = "sample-mission";

    private static final List<String> MISSIONS = List.of(MISSION_ID);

    private static final Mission MISSION = new Mission(
            MISSION_ID, "Sample Mission", "Can we build a ship that works?",
            "A made-up mission for development.", "Nothing real",
            Difficulty.EASY, Destination.EARTH_ORBIT, false,
            0, false, List.of(), List.of(),
            // Budget, deadline, science duration, delta-v, crew size.
            500,
            36, 12,
            1000,
            0,
            SOURCE);

    private static final List<ShipPart> PARTS = List.of(

            // id, slot, mass, power supply/draw, payload and bay, part size, thrust, Isp, tanks, data, cost
            part("sample-tiny-bus",      ModuleSlot.AIRFRAME,  500,  0,    20,  100,   0,   0,    0,   0,   0,     0,    200,   0,    60),
            part("sample-small-plane",   ModuleSlot.AIRFRAME,  1000, 0,    50,  200,   0,   0,    0,   0,   0,     0,    500,   0,    100),
            part("sample-medium-plane",  ModuleSlot.AIRFRAME,  2000, 0,    80,  500,   0,   0,    0,   0,   0,     0,    1000,  0,    150),
            part("sample-large-plane",   ModuleSlot.AIRFRAME,  4000, 0,    120, 1000,  0,   0,    0,   0,   0,     0,    2500,  0,    250),

            part("sample-small-thruster",  ModuleSlot.THRUSTERS, 20,  0,   0,   0,     0,   0,    0,   0,   400,   200,  0,     0,    5),
            part("sample-medium-thruster", ModuleSlot.THRUSTERS, 50,  0,   0,   0,     0,   0,    0,   0,   1000,  300,  0,     0,    10),
            part("sample-large-thruster",  ModuleSlot.THRUSTERS, 100, 0,   0,   0,     0,   0,    0,   0,   5000,  400,  0,     0,    20),
            part("sample-ion-thruster",    ModuleSlot.THRUSTERS, 30,  0,   400, 0,     0,   0,    0,   0,   1,     3000, 0,     0,    30),

            part("sample-body-solar",    ModuleSlot.POWER,     10,   100,  0,   0,     0,   0,    0,   0,   0,     0,    0,     0,    5),
            part("sample-solar-wings",   ModuleSlot.POWER,     50,   500,  0,   0,     0,   0,    0,   0,   0,     0,    0,     0,    15),
            part("sample-fuel-cell",     ModuleSlot.POWER,     100,  1000, 0,   0,     0,   0,    0,   0,   0,     0,    0,     0,    20),
            part("sample-generator",     ModuleSlot.POWER,     50,   200,  0,   0,     0,   0,    0,   0,   0,     0,    0,     0,    40),

            part("sample-small-dispenser", ModuleSlot.CARGO_BAY, 5,   0,   0,   10,    0.4, 0.2,  0,   0,   0,     0,    0,     0,    1),
            part("sample-large-dispenser", ModuleSlot.CARGO_BAY, 10,  0,   0,   30,    0.8, 0.3,  0,   0,   0,     0,    0,     0,    2),
            part("sample-medium-bay",      ModuleSlot.CARGO_BAY, 100, 0,   0,   400,   3,   1.5,  0,   0,   0,     0,    0,     0,    10),
            part("sample-large-bay",       ModuleSlot.CARGO_BAY, 300, 0,   0,   900,   6,   3,    0,   0,   0,     0,    0,     0,    20),

            part("sample-cube-1u",       ModuleSlot.SATELLITE, 2,    0,    5,   0,     0,   0,    0.1, 0.1, 0,     0,    0,     1,    1),
            part("sample-cube-3u",       ModuleSlot.SATELLITE, 5,    0,    10,  0,     0,   0,    0.3, 0.1, 0,     0,    0,     3,    3),
            part("sample-small-sat",     ModuleSlot.SATELLITE, 150,  0,    100, 0,     0,   0,    1.5, 1.0, 0,     0,    0,     50,   50),
            part("sample-big-sat",       ModuleSlot.SATELLITE, 800,  0,    300, 0,     0,   0,    5,   2.5, 0,     0,    0,     500,  200));

    private SampleData() {
    }

    public static Mission mission() {
        return MISSION;
    }


    public static List<ShipPart> parts() {
        return PARTS;
    }


    public static ShipPart part(String id) {
        return PARTS.stream()
                .filter(p -> p.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No sample part " + id));
    }


    public static ShipBuild starterBuild() {
        return ShipBuild.empty()
                .with(part("sample-medium-plane"))
                .with(part("sample-medium-thruster"))
                .with(part("sample-solar-wings"))
                .with(part("sample-medium-bay"))
                .with(part("sample-small-sat"));
    }

    private static ShipPart part(String id, ModuleSlot slot, double massKg,
                                 double powerSupplyW, double powerDrawW, double payloadMassKg,
                                 double bayLengthM, double bayDiameterM, double lengthM, double diameterM,
                                 double thrustN, double specificImpulseS, double propellantCapacityKg,
                                 double dataGbitPerDay, double costMusd) {
        String name = id.replace("sample-", "").replace('-', ' ');
        return new ShipPart(id, slot, name, name, "Sample part (not real)", "Nothing real", MISSIONS,
                massKg, powerSupplyW, powerDrawW,
                payloadMassKg, bayLengthM, bayDiameterM,
                lengthM, diameterM,
                thrustN, specificImpulseS, propellantCapacityKg,
                dataGbitPerDay, costMusd, ValueBasis.GAME_ESTIMATE,
                "model-" + id, SOURCE);
    }
}
