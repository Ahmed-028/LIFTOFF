package com.liftoff.engine;

/** The six resources every mission design is measured against. */
public enum ResourceType {
    MASS("kg"),
    POWER("W"),
    BUDGET("$M"),
    DATA("Gbit/day"),
    COMMS("min/day"),
    TIME("months");

    private final String unit;

    ResourceType(String unit) {
        this.unit = unit;
    }

    public String unit() {
        return unit;
    }
}
