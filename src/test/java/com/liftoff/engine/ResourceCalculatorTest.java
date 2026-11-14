package com.liftoff.engine;

import com.liftoff.TestParts;
import com.liftoff.model.Bus;
import com.liftoff.model.GroundStation;
import com.liftoff.model.Instrument;
import com.liftoff.model.LaunchClass;
import com.liftoff.model.LaunchVehicle;
import com.liftoff.model.MissionDesign;
import com.liftoff.model.Objective;
import com.liftoff.model.Orbit;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceCalculatorTest {


    private final Bus bus = TestParts.bus("bus", 400, 200, 10, 60, 30);
    private final Instrument camera = TestParts.instrument("camera", 50, 80, 1000, 25, 18);
    private final Instrument radar = TestParts.instrument("radar", 40, 100, 500, 20, 36);
    private final Orbit polarOrbit = TestParts.orbit("polar", 705, 90, LaunchClass.POLAR);
    private final GroundStation poleStation = TestParts.station("pole", 90, 0.5);
    private final GroundStation arcticStation = TestParts.station("arctic", 78, 0.5);
    private final LaunchVehicle rocket = TestParts.rocket("rocket", Map.of(LaunchClass.POLAR, 500.0), 100);
    private final Objective objective = TestParts.objective("goal", 300, 80, 24);

    private MissionDesign fullDesign() {
        return MissionDesign.empty()
                .withObjective(objective)
                .withBus(bus)
                .plusInstrument(camera)
                .plusInstrument(radar)
                .withOrbit(polarOrbit)
                .plusStation(poleStation)
                .plusStation(arcticStation)
                .withLaunchVehicle(rocket)
                .withScienceMonths(24);
    }

    private static ResourceUsage usage(MissionDesign design, ResourceType type) {
        return ResourceCalculator.calculate(design).get(type);
    }

    @Test
    void anEmptyDesignUsesNothingAndHasNoLimitsYet() {
        ResourceReport report = ResourceCalculator.calculate(MissionDesign.empty());

        for (ResourceType type : ResourceType.values()) {
            assertEquals(0.0, report.get(type).used(), 1e-9, type + " used");
            assertEquals(ResourceLevel.UNKNOWN, report.get(type).level(), type + " level");
        }
        assertFalse(report.anyOver());
    }

    @Test
    void massUsedIsTheBusPlusEveryInstrument() {
        assertEquals(490.0, usage(fullDesign(), ResourceType.MASS).used(), 1e-9);
    }

    @Test
    void massLimitIsWhatTheRocketLiftsToTheChosenClassOfOrbit() {
        ResourceUsage mass = usage(fullDesign(), ResourceType.MASS);

        assertEquals(500.0, mass.capacity().orElseThrow(), 1e-9);
        assertEquals(ResourceLevel.TIGHT, mass.level());
    }

    @Test
    void massLimitIsUnknownWhenTheRocketHasNoFigureForThatOrbit() {
        LaunchVehicle lowOrbitOnly = TestParts.rocket("small", Map.of(LaunchClass.LOW_INCLINATION, 450.0), 50);

        ResourceUsage mass = usage(fullDesign().withLaunchVehicle(lowOrbitOnly), ResourceType.MASS);

        assertTrue(mass.capacity().isEmpty());
        assertEquals(ResourceLevel.UNKNOWN, mass.level());
    }

    @Test
    void powerUsedIsTheInstrumentsAgainstWhatTheBusSupplies() {
        ResourceUsage power = usage(fullDesign(), ResourceType.POWER);

        assertEquals(180.0, power.used(), 1e-9);
        assertEquals(200.0, power.capacity().orElseThrow(), 1e-9);
        assertEquals(ResourceLevel.TIGHT, power.level());
    }

    @Test
    void powerGoesOverWhenInstrumentsDrawMoreThanTheBusSupplies() {
        Instrument hungry = TestParts.instrument("hungry", 10, 50, 1, 5, 18);

        ResourceUsage power = usage(fullDesign().plusInstrument(hungry), ResourceType.POWER);

        assertEquals(230.0, power.used(), 1e-9);
        assertEquals(ResourceLevel.OVER, power.level());
    }

    @Test
    void budgetAddsBusInstrumentsRocketAndStationsForEveryScienceMonth() {

        ResourceUsage budget = usage(fullDesign(), ResourceType.BUDGET);

        assertEquals(229.0, budget.used(), 1e-9);
        assertEquals(300.0, budget.capacity().orElseThrow(), 1e-9);
        assertEquals(ResourceLevel.OK, budget.level());
    }

    @Test
    void dataUsedIsWhatTheInstrumentsProduceInADay() {
        assertEquals(129.6, usage(fullDesign(), ResourceType.DATA).used(), 1e-9);
    }

    @Test
    void dataLimitIsTheDownlinkRateTimesTheDailyContactTime() {
        double contactMinutes = OrbitMath.contactMinutesPerDay(polarOrbit, 90)
                + OrbitMath.contactMinutesPerDay(polarOrbit, 78);

        double expectedGbit = 10 * 60 * contactMinutes / 1000;

        assertEquals(expectedGbit, usage(fullDesign(), ResourceType.DATA).capacity().orElseThrow(), 1e-6);
    }

    @Test
    void dataIsOverWhenNoGroundStationIsChosen() {
        MissionDesign noStations = fullDesign().minusStation("pole").minusStation("arctic");

        ResourceUsage data = usage(noStations, ResourceType.DATA);

        assertEquals(0.0, data.capacity().orElseThrow(), 1e-9);
        assertEquals(ResourceLevel.OVER, data.level());
    }

    @Test
    void commsNeededIsTheMinutesToSendTheDailyDataAtTheDownlinkRate() {

        assertEquals(216.0, usage(fullDesign(), ResourceType.COMMS).used(), 1e-9);
    }

    @Test
    void commsLimitIsTheContactTimeOfAllChosenStations() {
        double expected = OrbitMath.contactMinutesPerDay(polarOrbit, 90)
                + OrbitMath.contactMinutesPerDay(polarOrbit, 78);

        assertEquals(expected, usage(fullDesign(), ResourceType.COMMS).capacity().orElseThrow(), 1e-6);
    }

    @Test
    void timeIsTheSlowestPartPlusIntegrationPlusTheScienceDuration() {

        ResourceUsage time = usage(fullDesign(), ResourceType.TIME);

        assertEquals(70.0, time.used(), 1e-9);
        assertEquals(80.0, time.capacity().orElseThrow(), 1e-9);
        assertEquals(ResourceLevel.TIGHT, time.level());
    }

    @Test
    void theReportSaysWhenAnythingIsOver() {
        Instrument hungry = TestParts.instrument("hungry", 10, 50, 1, 5, 18);

        assertTrue(ResourceCalculator.calculate(fullDesign().plusInstrument(hungry)).anyOver());
    }
}
