package com.liftoff;

import com.liftoff.model.ModuleSlot;
import com.liftoff.model.ShipBuild;
import com.liftoff.model.ShipPart;
import com.liftoff.model.ValueBasis;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SampleDataTest {

    @Test
    void everyTabHasAtLeastFourOptions() {

        for (ModuleSlot slot : ModuleSlot.values()) {
            long count = SampleData.parts().stream().filter(p -> p.slot() == slot).count();
            assertTrue(count >= 4, slot + " has only " + count + " options");
        }
    }

    @Test
    void idsAreUnique() {
        Set<String> seen = new HashSet<>();
        for (ShipPart part : SampleData.parts()) {
            assertTrue(seen.add(part.id()), "duplicate id " + part.id());
        }
    }

    @Test
    void nothingCanBeMistakenForNasaData() {

        for (ShipPart part : SampleData.parts()) {
            assertEquals(ValueBasis.GAME_ESTIMATE, part.costBasis(), part.id());
            assertEquals(SampleData.SOURCE, part.source(), part.id());
        }
        assertEquals(SampleData.SOURCE, SampleData.mission().source());
        assertTrue(SampleData.SOURCE.title().contains("not real"));
    }

    @Test
    void everyPartIsOfferedInTheSampleMissionAndHasAModel() {
        for (ShipPart part : SampleData.parts()) {
            assertTrue(part.offeredIn(SampleData.mission().id()), part.id());
            assertFalse(part.modelId().isBlank(), part.id());
        }
    }

    @Test
    void slotsThatNeedSizesHaveThem() {
        for (ShipPart part : SampleData.parts()) {
            switch (part.slot()) {
                case AIRFRAME -> assertTrue(part.payloadMassKg() > 0 && part.propellantCapacityKg() > 0, part.id());
                case THRUSTERS -> assertTrue(part.thrustN() > 0 && part.specificImpulseS() > 0, part.id());
                case POWER -> assertTrue(part.powerSupplyW() > 0, part.id());
                case CARGO_BAY -> assertTrue(part.bayLengthM() > 0 && part.bayDiameterM() > 0
                        && part.payloadMassKg() > 0, part.id());
                case SATELLITE -> assertTrue(part.lengthM() > 0 && part.diameterM() > 0, part.id());
            }
            assertTrue(part.massKg() > 0, part.id());
        }
    }

    @Test
    void findsPartsById() {
        assertEquals(ModuleSlot.POWER, SampleData.part("sample-solar-wings").slot());
        assertThrows(IllegalArgumentException.class, () -> SampleData.part("no-such-part"));
    }

    @Test
    void starterBuildIsCompleteAndUsesOnePartPerTab() {
        ShipBuild starter = SampleData.starterBuild();
        assertTrue(starter.isComplete());
        assertEquals(List.of(ModuleSlot.values()), List.copyOf(starter.parts().keySet()));
    }
}
