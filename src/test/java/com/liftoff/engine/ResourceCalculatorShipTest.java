package com.liftoff.engine;

import com.liftoff.TestParts;
import com.liftoff.model.Bus;
import com.liftoff.model.Instrument;
import com.liftoff.model.Mission;
import com.liftoff.model.MissionDesign;
import com.liftoff.model.Objective;
import com.liftoff.model.ShipBuild;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The six gauges once the ship is built from parts (v2). Same round numbers as BuildCalculatorTest:
 * ship dry mass 1300 kg, cost $185M, parts draw 150 W, power system gives 200 W, Isp 300 s.
 * The mission's delta-v is Isp x g0 x ln 2, so propellant equals dry mass.
 */
class ResourceCalculatorShipTest {

    private static final double TINY = 1e-6;
    private static final double ISP = 300;
    private static final double DELTA_V = ISP * 9.80665 * Math.log(2);

    private static final Mission MISSION = TestParts.mission("trip", 400, DELTA_V);   // deadline 36 months

    /** 100 kg, 30 W, no data, $40M, 10 build months. */
    private static final Instrument CAMERA = TestParts.instrument("camera", 100, 30, 0, 40, 10);

    private static ShipBuild ship() {
        return ShipBuild.empty()
                .with(TestParts.airframe("plane", 1000, 500, 2000, 50, 100))
                .with(TestParts.thruster("engine", 100, ISP, 0, 10))
                .with(TestParts.power("solar", 50, 200, 20))
                .with(TestParts.cargoBay("bay", 50, 300, 3, 2, 5))
                .with(TestParts.satellite("sat", 100, 2, 1, 100, 50));
    }

    private static MissionDesign shipDesign() {
        return MissionDesign.empty().withMission(MISSION).withBuild(ship());
    }

    private static ResourceUsage usage(MissionDesign design, ResourceType type) {
        return ResourceCalculator.calculate(design).get(type);
    }

    @Test
    void massIsShipPartsPlusPropellant() {
        // dry 1300 + propellant 1300
        assertEquals(2600, usage(shipDesign(), ResourceType.MASS).used(), TINY);
    }

    @Test
    void anInstrumentAddsItsMassAndTheFuelToCarryIt() {
        // dry 1300 + 100 = 1400, propellant 1400: 100 kg of camera costs 200 kg on the gauge.
        assertEquals(2800, usage(shipDesign().plusInstrument(CAMERA), ResourceType.MASS).used(), TINY);
    }

    @Test
    void withoutAThrusterThereIsNoPropellantYet() {
        ShipBuild noEngine = ShipBuild.empty()
                .with(TestParts.airframe("plane", 1000, 500, 2000, 50, 100))
                .with(TestParts.power("solar", 50, 200, 20));
        MissionDesign design = MissionDesign.empty().withMission(MISSION).withBuild(noEngine);
        assertEquals(1050, usage(design, ResourceType.MASS).used(), TINY);
    }

    @Test
    void withoutAMissionThereIsNoTripSoNoPropellant() {
        assertEquals(1300, usage(MissionDesign.empty().withBuild(ship()), ResourceType.MASS).used(), TINY);
    }

    @Test
    void powerLimitComesFromThePowerPart() {
        ResourceUsage power = usage(shipDesign(), ResourceType.POWER);
        assertEquals(150, power.used(), TINY);
        assertEquals(OptionalDouble.of(200), power.capacity());
        assertEquals(ResourceLevel.OK, power.level());
    }

    @Test
    void instrumentsDrawFromTheSamePowerSystem() {
        // 150 W of parts + 30 W of camera = 180 W of 200 W: inside, but under 15 % margin.
        ResourceUsage power = usage(shipDesign().plusInstrument(CAMERA), ResourceType.POWER);
        assertEquals(180, power.used(), TINY);
        assertEquals(ResourceLevel.TIGHT, power.level());
    }

    @Test
    void thePowerPartWinsOverAnOldStyleBus() {
        Bus bus = TestParts.bus("bus", 400, 999, 10, 60, 24);
        ResourceUsage power = usage(shipDesign().withBus(bus), ResourceType.POWER);
        assertEquals(OptionalDouble.of(200), power.capacity());
    }

    @Test
    void aShipWithNoPowerPartHasNoPowerLimitYet() {
        ShipBuild noPower = ShipBuild.empty().with(TestParts.airframe("plane", 1000, 500, 2000, 50, 100));
        ResourceUsage power = usage(MissionDesign.empty().withBuild(noPower), ResourceType.POWER);
        assertEquals(50, power.used(), TINY);
        assertEquals(ResourceLevel.UNKNOWN, power.level());
    }

    @Test
    void budgetAddsTheShipPartsAgainstTheMissionCap() {
        // parts 185 + camera 40 = 225 of 400
        ResourceUsage budget = usage(shipDesign().plusInstrument(CAMERA), ResourceType.BUDGET);
        assertEquals(225, budget.used(), TINY);
        assertEquals(OptionalDouble.of(400), budget.capacity());
    }

    @Test
    void timeIsMeasuredAgainstTheMissionDeadline() {
        // camera 10 + integration 6 + 2 per instrument = 18, plus 12 science months = 30 of 36
        ResourceUsage time = usage(shipDesign().plusInstrument(CAMERA).withScienceMonths(12), ResourceType.TIME);
        assertEquals(30, time.used(), TINY);
        assertEquals(OptionalDouble.of(36), time.capacity());
    }

    @Test
    void theMissionSetsTheCapsWhenBothMissionAndObjectiveArePresent() {
        Objective objective = TestParts.objective("old", 999, 99, 12);
        MissionDesign design = shipDesign().withObjective(objective);
        assertEquals(OptionalDouble.of(400), usage(design, ResourceType.BUDGET).capacity());
        assertEquals(OptionalDouble.of(36), usage(design, ResourceType.TIME).capacity());
    }

    @Test
    void theSatellitesOwnDataCountsTowardDataPerDay() {
        // SampleData-style satellite that makes 50 Gbit/day by itself.
        var chatty = new com.liftoff.model.ShipPart("chatty", com.liftoff.model.ModuleSlot.SATELLITE, "chatty",
                "chatty", "", "", java.util.List.of(), 100, 0, 100, 0, 0, 0, 2, 1, 0, 0, 0, 50, 50,
                com.liftoff.model.ValueBasis.GAME_ESTIMATE, "model-chatty", TestParts.SOURCE);
        assertEquals(50, usage(shipDesign().withPart(chatty), ResourceType.DATA).used(), TINY);
    }
}
