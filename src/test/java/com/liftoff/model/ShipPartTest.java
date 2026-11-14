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


    private static ShipPart part(String id, ModuleSlot slot, List<String> missions, SourceRef source) {
        return new ShipPart(id, slot, id, id, "", "", missions,
                0, 0, 0,
                0, 0, 0,
                0, 0,
                0, 0, 0,
                0, 0, ValueBasis.GAME_ESTIMATE,
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
        // Reject parts without sources so their values stay traceable.
        assertThrows(NullPointerException.class,
                () -> part("p", ModuleSlot.POWER, List.of(), null));
    }

    @Test
    void missionListCannotBeChangedFromOutside() {
        List<String> missions = new ArrayList<>(List.of("farm-watch"));
        ShipPart p = part("p", ModuleSlot.POWER, missions, TestParts.SOURCE);
        missions.add("moon-landing");
        assertEquals(List.of("farm-watch"), p.missions());
    }

    @Test
    void missionListReturnedCannotBeEdited() {
        ShipPart p = part("p", ModuleSlot.POWER, List.of("farm-watch"), TestParts.SOURCE);
        assertThrows(UnsupportedOperationException.class, () -> p.missions().add("mars-drones"));
    }


    private static ShipPart withNumber(ModuleSlot slot, int position, double value) {
        double[] n = new double[13];
        if (slot == ModuleSlot.THRUSTERS) {
            n[9] = 300;
        }
        n[position] = value;
        return new ShipPart("p", slot, "p", "p", "", "", List.of(),
                n[0], n[1], n[2], n[3], n[4], n[5], n[6], n[7], n[8], n[9], n[10], n[11],
                n[12], ValueBasis.GAME_ESTIMATE, "model-p", TestParts.SOURCE);
    }

    @Test
    void rejectsANegativeNumberInAnyField() {


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
        // Fuel calculations require every thruster to specify Isp.
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
