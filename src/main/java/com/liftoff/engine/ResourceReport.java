package com.liftoff.engine;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** The six resources of one design, always listed in the order of {@link ResourceType}. */
public record ResourceReport(Map<ResourceType, ResourceUsage> usages) {

    public ResourceReport {
        usages = Collections.unmodifiableMap(new EnumMap<>(usages));
    }

    public ResourceUsage get(ResourceType type) {
        return usages.get(type);
    }

    /** True when at least one resource is over its limit. */
    public boolean anyOver() {
        return usages.values().stream().anyMatch(usage -> usage.level() == ResourceLevel.OVER);
    }
}
