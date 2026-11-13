package com.liftoff.model;

import com.liftoff.TestParts;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShipPartTest {

    /** Builds a part with only the fields these tests care about; every number is 0. */
    private static ShipPart part(String id, ModuleSlot slot, List<String> missions, SourceRef source) {
        return new ShipPart(id, slot, id, id, "", "", missions,
                0, 0, 0,          // massKg, powerSupplyW, powerDrawW
                0, 0, 0,          // payloadMassKg, bayLengthM, bayDiameterM
                0, 0,             // lengthM, diameterM
                0, 0, 0,          // thrustN, specificImpulseS, propellantCapacityKg
                0, 0, ValueBasis.GAME_ESTIMATE,   // dataGbitPerDay, costMusd, costBasis
                "model-" + id, source);
    }

    @Test
    void rejectsMissingId() {
        assertThrows(NullPointerException.class,
                () -> part(null, ModuleSlot.POWER, List.of(), TestParts.SOURCE));
    }

    @Test
    void rejectsMissingSlot() {
        assertThrows(NullPointerException.class,
                () -> part("p", null, List.of(), TestParts.SOURCE));
    }

    @Test
    void rejectsMissingSource() {
        // A part without a source would break the data rule: every number must say where it comes from.
        assertThrows(NullPointerException.class,
                () -> part("p", ModuleSlot.POWER, List.of(), null));
    }

    @Test
    void missionListCannotBeChangedFromOutside() {
        List<String> missions = new ArrayList<>(List.of("farm-watch"));
        ShipPart p = part("p", ModuleSlot.POWER, missions, TestParts.SOURCE);
        missions.add("moon-landing");                       // change the original list...
        assertEquals(List.of("farm-watch"), p.missions());  // ...the part must not change
    }

    @Test
    void missionListReturnedCannotBeEdited() {
        ShipPart p = part("p", ModuleSlot.POWER, List.of("farm-watch"), TestParts.SOURCE);
        assertThrows(UnsupportedOperationException.class, () -> p.missions().add("mars-drones"));
    }

    /** A part where one number is set and the rest are 0; position counts from massKg = 0. */
    private static ShipPart withNumber(ModuleSlot slot, int position, double value) {
        double[] n = new double[13];
        if (slot == ModuleSlot.THRUSTERS) {
            n[9] = 300;   // a thruster always needs a specific impulse
        }
        n[position] = value;
        return new ShipPart("p", slot, "p", "p", "", "", List.of(),
                n[0], n[1], n[2], n[3], n[4], n[5], n[6], n[7], n[8], n[9], n[10], n[11],
                n[12], ValueBasis.GAME_ESTIMATE, "model-p", TestParts.SOURCE);
    }

    @Test
    void rejectsANegativeNumberInAnyField() {
        // massKg, powerSupplyW, powerDrawW, payloadMassKg, bayLengthM, bayDiameterM, lengthM, diameterM,
        // thrustN, specificImpulseS, propellantCapacityKg, dataGbitPerDay, costMusd
        for (int position = 0; position < 13; position++) {
            int field = position;
            assertThrows(IllegalArgumentException.class,
                    () -> withNumber(ModuleSlot.POWER, field, -1), "field " + position);
        }
    }

    @Test
    void rejectsANumberThatIsNotANumber() {
        assertThrows(IllegalArgumentException.class, () -> withNumber(ModuleSlot.POWER, 0, Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> withNumber(ModuleSlot.POWER, 0, Double.POSITIVE_INFINITY));
    }

    @Test
    void aThrusterMustHaveASpecificImpulse() {
        // The rocket equation divides by Isp: a thruster without one cannot be worked out.
        assertThrows(IllegalArgumentException.class, () -> withNumber(ModuleSlot.THRUSTERS, 9, 0));
        assertEquals(300, withNumber(ModuleSlot.THRUSTERS, 0, 20).specificImpulseS());
    }

    @Test
    void knowsWhichMissionsOfferIt() {
        ShipPart p = part("p", ModuleSlot.POWER, List.of("farm-watch"), TestParts.SOURCE);
        assertTrue(p.offeredIn("farm-watch"));
        assertFalse(p.offeredIn("mars-drones"));
    }
}
