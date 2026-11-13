package com.liftoff.engine;

import java.util.Map;

/** The six resources of one design. */
public record ResourceReport(Map<ResourceType, ResourceUsage> usages) {

    public ResourceReport {
        usages = Map.copyOf(usages);
    }

    public ResourceUsage get(ResourceType type) {
        return usages.get(type);
    }

    /** True when at least one resource is over its limit. */
    public boolean anyOver() {
        return usages.values().stream().anyMatch(usage -> usage.level() == ResourceLevel.OVER);
    }
}
