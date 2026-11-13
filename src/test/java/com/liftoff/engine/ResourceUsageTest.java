package com.liftoff.engine;

import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceUsageTest {

    private static ResourceLevel levelOf(double used, double capacity) {
        return ResourceUsage.of(ResourceType.MASS, used, OptionalDouble.of(capacity)).level();
    }

    @Test
    void wellInsideTheLimitIsOk() {
        assertEquals(ResourceLevel.OK, levelOf(84, 100));
    }

    @Test
    void lessThanFifteenPercentMarginIsTight() {
        assertEquals(ResourceLevel.TIGHT, levelOf(85, 100));
    }

    @Test
    void exactlyAtTheLimitIsStillTightNotOver() {
        assertEquals(ResourceLevel.TIGHT, levelOf(100, 100));
    }

    @Test
    void aboveTheLimitIsOver() {
        assertEquals(ResourceLevel.OVER, levelOf(100.5, 100));
    }

    @Test
    void anythingUsedAgainstAZeroLimitIsOver() {
        assertEquals(ResourceLevel.OVER, levelOf(1, 0));
    }

    @Test
    void nothingUsedAgainstAZeroLimitIsOk() {
        assertEquals(ResourceLevel.OK, levelOf(0, 0));
    }

    @Test
    void withoutALimitTheLevelIsUnknown() {
        ResourceUsage usage = ResourceUsage.of(ResourceType.MASS, 50, OptionalDouble.empty());

        assertEquals(ResourceLevel.UNKNOWN, usage.level());
        assertEquals(50, usage.used(), 1e-9);
    }
}
