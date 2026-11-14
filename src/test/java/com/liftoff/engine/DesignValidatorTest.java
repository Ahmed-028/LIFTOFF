package com.liftoff.engine;

import com.liftoff.TestParts;
import com.liftoff.model.Capability;
import com.liftoff.model.Instrument;
import com.liftoff.model.LaunchClass;
import com.liftoff.model.MeasurementType;
import com.liftoff.model.Mission;
import com.liftoff.model.MissionDesign;
import com.liftoff.model.Objective;
import com.liftoff.model.Orbit;
import com.liftoff.model.Requirement;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DesignValidatorTest {


    private static final Requirement LAND_30M = new Requirement(MeasurementType.LAND_IMAGING, 30.0);

    private static final Instrument SHARP = camera("sharp", 30.0);
    private static final Instrument SHARPER = camera("sharper", 15.0);
    private static final Instrument COARSE = camera("coarse", 250.0);

    private static final Orbit POLAR = TestParts.orbit("polar", 705, 98.2, LaunchClass.POLAR);
    private static final Orbit TROPICAL = TestParts.orbit("tropical", 402, 35, LaunchClass.LOW_INCLINATION);

    private static Instrument camera(String id, Double resolutionM) {
        return TestParts.instrument(id, List.of(new Capability(MeasurementType.LAND_IMAGING, resolutionM)));
    }

    private static Mission farmMission(double targetLatitudeDeg) {
        return TestParts.mission("farm", 500, 0, targetLatitudeDeg, List.of(LAND_30M));
    }

    private static MissionDesign design(Mission mission, Orbit orbit, Instrument... instruments) {
        MissionDesign design = MissionDesign.empty().withMission(mission).withOrbit(orbit);
        for (Instrument instrument : instruments) {
            design = design.plusInstrument(instrument);
        }
        return design;
    }

    @Test
    void aDesignThatMeetsTheGoalRaisesNothing() {
        assertEquals(List.of(), DesignValidator.validate(design(farmMission(50), POLAR, SHARP)));
    }

    @Test
    void measurementMissing() {
        Instrument thermometer = TestParts.instrument("thermo",
                List.of(new Capability(MeasurementType.THERMAL_IMAGING, 60.0)));
        assertEquals(List.of(new DesignIssue(DesignIssue.Kind.MEASUREMENT_MISSING, MeasurementType.LAND_IMAGING, 30, 0)),
                DesignValidator.validate(design(farmMission(50), POLAR, thermometer)));
    }

    @Test
    void noInstrumentsAtAllMeansTheMeasurementIsMissing() {
        List<DesignIssue> issues = DesignValidator.validate(design(farmMission(50), POLAR));
        assertEquals(1, issues.size());
        assertEquals(DesignIssue.Kind.MEASUREMENT_MISSING, issues.get(0).kind());
    }

    @Test
    void resolutionTooCoarse() {

        assertEquals(List.of(new DesignIssue(DesignIssue.Kind.RESOLUTION_TOO_COARSE, MeasurementType.LAND_IMAGING, 30, 250)),
                DesignValidator.validate(design(farmMission(50), POLAR, COARSE)));
    }

    @Test
    void oneSharpEnoughInstrumentIsEnough() {
        assertEquals(List.of(), DesignValidator.validate(design(farmMission(50), POLAR, COARSE, SHARPER)));
    }

    @Test
    void tooCoarseReportsTheBestTheDesignCanDo() {
        Instrument blurry = camera("blurry", 1000.0);
        List<DesignIssue> issues = DesignValidator.validate(design(farmMission(50), POLAR, blurry, COARSE));
        assertEquals(250, issues.get(0).available());
    }

    @Test
    void anUnstatedResolutionCannotProveTheGoalIsMet() {
        // Data rule: if the source gives no resolution, we do not assume it is sharp enough.
        List<DesignIssue> issues = DesignValidator.validate(design(farmMission(50), POLAR, camera("unknown", null)));
        assertEquals(1, issues.size());
        assertEquals(DesignIssue.Kind.RESOLUTION_TOO_COARSE, issues.get(0).kind());
        assertTrue(Double.isNaN(issues.get(0).available()));
    }

    @Test
    void aRequirementWithNoResolutionLimitOnlyNeedsTheMeasurement() {
        Mission anyLandPicture = TestParts.mission("any", 500, 0, 50,
                List.of(new Requirement(MeasurementType.LAND_IMAGING, null)));
        assertEquals(List.of(), DesignValidator.validate(design(anyLandPicture, POLAR, camera("unknown", null))));
    }

    @Test
    void orbitMissesTarget() {

        assertEquals(List.of(new DesignIssue(DesignIssue.Kind.ORBIT_MISSES_TARGET, null, 50, 35)),
                DesignValidator.validate(design(farmMission(50), TROPICAL, SHARP)));
    }

    @Test
    void aRetrogradeOrbitReachesOneEightyMinusItsInclination() {

        assertEquals(List.of(), DesignValidator.validate(design(farmMission(80), POLAR, SHARP)));
        List<DesignIssue> issues = DesignValidator.validate(design(farmMission(90), POLAR, SHARP));
        assertEquals(DesignIssue.Kind.ORBIT_MISSES_TARGET, issues.get(0).kind());
        assertEquals(81.8, issues.get(0).available(), 1e-9);
    }

    @Test
    void southernTargetsCountTheSame() {
        assertEquals(List.of(new DesignIssue(DesignIssue.Kind.ORBIT_MISSES_TARGET, null, 50, 35)),
                DesignValidator.validate(design(farmMission(-50), TROPICAL, SHARP)));
    }

    @Test
    void noOrbitChosenYetSkipsTheOrbitCheck() {
        assertEquals(List.of(), DesignValidator.validate(design(farmMission(50), null, SHARP)));
    }

    @Test
    void everyProblemIsReportedMeasurementsFirst() {
        List<DesignIssue.Kind> kinds = DesignValidator.validate(design(farmMission(50), TROPICAL, COARSE))
                .stream().map(DesignIssue::kind).toList();
        assertEquals(List.of(DesignIssue.Kind.RESOLUTION_TOO_COARSE, DesignIssue.Kind.ORBIT_MISSES_TARGET), kinds);
    }

    @Test
    void noGoalChosenMeansNothingToCheck() {
        assertEquals(List.of(), DesignValidator.validate(MissionDesign.empty()));
    }

    @Test
    void worksFromAnOldStyleObjectiveToo() {
        Objective objective = new Objective("old", "old", "", "", "", 50, false, List.of(LAND_30M), List.of(),
                500, 36, 12, TestParts.SOURCE);
        MissionDesign design = MissionDesign.empty().withObjective(objective).withOrbit(POLAR).plusInstrument(COARSE);
        assertEquals(DesignIssue.Kind.RESOLUTION_TOO_COARSE, DesignValidator.validate(design).get(0).kind());
    }
}
