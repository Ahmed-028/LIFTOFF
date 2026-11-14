package com.liftoff.session.stub;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;


public record ShipBuild(Map<ModuleSlot, ShipPart> parts) {

    public ShipBuild {
        Map<ModuleSlot, ShipPart> copy = new EnumMap<>(ModuleSlot.class);
        copy.putAll(parts);
        parts = Collections.unmodifiableMap(copy);
    }

    public static ShipBuild empty() {
        return new ShipBuild(Map.of());
    }


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
