package com.liftoff.session.stub;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/** TODO(Member 2): stub of model/ShipBuild — what is chosen in each tab; immutable. */
public record ShipBuild(Map<ModuleSlot, ShipPart> parts) {

    public ShipBuild {
        Map<ModuleSlot, ShipPart> copy = new EnumMap<>(ModuleSlot.class);
        copy.putAll(parts);
        parts = Collections.unmodifiableMap(copy);
    }

    public static ShipBuild empty() {
        return new ShipBuild(Map.of());
    }

    /** Replaces the part in that part's slot only. */
    public ShipBuild with(ShipPart part) {
        Map<ModuleSlot, ShipPart> changed = new EnumMap<>(ModuleSlot.class);
        changed.putAll(parts);
        changed.put(part.slot(), part);
        return new ShipBuild(changed);
    }

    public Optional<ShipPart> part(ModuleSlot slot) {
        return Optional.ofNullable(parts.get(slot));
    }

    public boolean isComplete() {
        return parts.size() == ModuleSlot.values().length;
    }
}
