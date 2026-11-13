package com.liftoff.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * The ship the player is building: at most one part in each of the five slots.
 * Immutable: with() never changes this build, it returns a new one.
 */
public record ShipBuild(Map<ModuleSlot, ShipPart> parts) {

    public ShipBuild {
        Map<ModuleSlot, ShipPart> copy = new EnumMap<>(ModuleSlot.class);
        for (Map.Entry<ModuleSlot, ShipPart> entry : parts.entrySet()) {
            ModuleSlot slot = entry.getKey();
            ShipPart part = entry.getValue();
            if (slot != part.slot()) {
                throw new IllegalArgumentException(
                        "Part " + part.id() + " belongs in " + part.slot() + ", not " + slot);
            }
            copy.put(slot, part);
        }
        parts = Collections.unmodifiableMap(copy);
    }

    /** A ship with nothing chosen yet. */
    public static ShipBuild empty() {
        return new ShipBuild(Map.of());
    }

    /** A new build with this part in its slot; every other slot keeps its part. */
    public ShipBuild with(ShipPart part) {
        Map<ModuleSlot, ShipPart> changed = new EnumMap<>(ModuleSlot.class);
        changed.putAll(parts);
        changed.put(part.slot(), part);
        return new ShipBuild(changed);
    }

    /** The part in that slot, or empty if the player has not chosen one yet. */
    public Optional<ShipPart> part(ModuleSlot slot) {
        return Optional.ofNullable(parts.get(slot));
    }

    /** True only when all five slots have a part. */
    public boolean isComplete() {
        return parts.size() == ModuleSlot.values().length;
    }
}
