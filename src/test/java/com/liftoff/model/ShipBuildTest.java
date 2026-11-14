package com.liftoff.model;

import com.liftoff.TestParts;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShipBuildTest {


    private static ShipPart part(String id, ModuleSlot slot) {
        double specificImpulseS = slot == ModuleSlot.THRUSTERS ? 300 : 0;
        return new ShipPart(id, slot, id, id, "", "", List.of(),
                0, 0, 0, 0, 0, 0, 0, 0, 0, specificImpulseS, 0, 0,
                0, ValueBasis.GAME_ESTIMATE, "model-" + id, TestParts.SOURCE);
    }


    private static ShipBuild fullBuild() {
        ShipBuild build = ShipBuild.empty();
        for (ModuleSlot slot : ModuleSlot.values()) {
            build = build.with(part(slot.name().toLowerCase(), slot));
        }
        return build;
    }

    @Test
    void emptyBuildHasNoParts() {
        ShipBuild empty = ShipBuild.empty();
        assertTrue(empty.parts().isEmpty());
        assertTrue(empty.part(ModuleSlot.AIRFRAME).isEmpty());
        assertFalse(empty.isComplete());
    }

    @Test
    void withPutsThePartInItsOwnSlot() {
        ShipPart power = part("solar", ModuleSlot.POWER);
        ShipBuild build = ShipBuild.empty().with(power);
        assertSame(power, build.part(ModuleSlot.POWER).orElseThrow());
        assertEquals(1, build.parts().size());
    }

    @Test
    void swappingOnePartKeepsEveryOtherPart() {

        ShipPart airframe = part("plane", ModuleSlot.AIRFRAME);
        ShipPart oldPower = part("solar", ModuleSlot.POWER);
        ShipPart newPower = part("fuel-cell", ModuleSlot.POWER);

        ShipBuild swapped = ShipBuild.empty().with(airframe).with(oldPower).with(newPower);

        assertSame(airframe, swapped.part(ModuleSlot.AIRFRAME).orElseThrow());
        assertSame(newPower, swapped.part(ModuleSlot.POWER).orElseThrow());
        assertEquals(2, swapped.parts().size());
    }

    @Test
    void withReturnsANewBuildAndLeavesTheOldOneAlone() {
        ShipBuild before = ShipBuild.empty();
        ShipBuild after = before.with(part("solar", ModuleSlot.POWER));
        assertTrue(before.parts().isEmpty());
        assertEquals(1, after.parts().size());
    }

    @Test
    void completeOnlyWhenAllFiveSlotsAreFilled() {
        ShipBuild full = fullBuild();
        assertTrue(full.isComplete());

        ShipBuild missingOne = ShipBuild.empty();
        for (ModuleSlot slot : List.of(ModuleSlot.AIRFRAME, ModuleSlot.THRUSTERS,
                ModuleSlot.POWER, ModuleSlot.CARGO_BAY)) {
            missingOne = missingOne.with(part(slot.name(), slot));
        }
        assertFalse(missingOne.isComplete());
    }

    @Test
    void partsComeOutInTabOrder() {

        ShipBuild build = ShipBuild.empty()
                .with(part("sat", ModuleSlot.SATELLITE))
                .with(part("plane", ModuleSlot.AIRFRAME));
        assertEquals(List.of(ModuleSlot.AIRFRAME, ModuleSlot.SATELLITE), List.copyOf(build.parts().keySet()));
    }

    @Test
    void partsMapCannotBeEdited() {
        ShipBuild build = fullBuild();
        assertThrows(UnsupportedOperationException.class,
                () -> build.parts().remove(ModuleSlot.POWER));
    }

    @Test
    void mapPassedInCannotChangeTheBuildLater() {
        Map<ModuleSlot, ShipPart> map = new EnumMap<>(ModuleSlot.class);
        map.put(ModuleSlot.POWER, part("solar", ModuleSlot.POWER));
        ShipBuild build = new ShipBuild(map);
        map.put(ModuleSlot.AIRFRAME, part("plane", ModuleSlot.AIRFRAME));
        assertEquals(1, build.parts().size());
    }

    @Test
    void rejectsAPartStoredUnderTheWrongSlot() {
        Map<ModuleSlot, ShipPart> wrong = Map.of(ModuleSlot.POWER, part("plane", ModuleSlot.AIRFRAME));
        assertThrows(IllegalArgumentException.class, () -> new ShipBuild(wrong));
    }
}
