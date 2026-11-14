package com.liftoff.engine;

import java.util.OptionalDouble;


public record ResourceUsage(ResourceType type, double used, OptionalDouble capacity, ResourceLevel level) {


    public static ResourceUsage of(ResourceType type, double used, OptionalDouble capacity) {
        return new ResourceUsage(type, used, capacity, levelFor(used, capacity));
    }

    private static ResourceLevel levelFor(double used, OptionalDouble capacity) {
        if (capacity.isEmpty()) {
            return ResourceLevel.UNKNOWN;
        }
        double limit = capacity.getAsDouble();
        if (used > limit) {
            return ResourceLevel.OVER;
        }
        if (limit > 0 && used >= GameRules.TIGHT_FRACTION * limit) {
            return ResourceLevel.TIGHT;
        }
        return ResourceLevel.OK;
    }
}
