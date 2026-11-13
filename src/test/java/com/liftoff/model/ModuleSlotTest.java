package com.liftoff.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ModuleSlotTest {

    @Test
    void slotsAreInTabOrder() {
        // The builder shows its tabs top to bottom in this order (doc 05 §4).
        assertArrayEquals(
                new ModuleSlot[] {
                        ModuleSlot.AIRFRAME, ModuleSlot.THRUSTERS, ModuleSlot.POWER,
                        ModuleSlot.CARGO_BAY, ModuleSlot.SATELLITE },
                ModuleSlot.values());
    }
}
