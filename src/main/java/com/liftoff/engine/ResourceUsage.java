package com.liftoff.engine;

import java.util.OptionalDouble;

/**
 * One resource of a design: how much is used, against what limit.
 *
 * @param capacity the limit; empty while the part that sets it has not been chosen
 */
public record ResourceUsage(ResourceType type, double used, OptionalDouble capacity, ResourceLevel level) {

    /** Builds a usage and works out its level from the amounts. */
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
