package com.liftoff.engine;

import java.util.Map;


public record ResourceReport(Map<ResourceType, ResourceUsage> usages) {

    public ResourceReport {
        usages = Map.copyOf(usages);
    }

    public ResourceUsage get(ResourceType type) {
        return usages.get(type);
    }


    public boolean anyOver() {
        return usages.values().stream().anyMatch(usage -> usage.level() == ResourceLevel.OVER);
    }
}
