package com.liftoff.engine;

import com.liftoff.model.ModuleSlot;

import java.util.Objects;


public record BuildIssue(Kind kind, ModuleSlot slot, double needed, double available) {

    public enum Kind {

        SLOT_EMPTY,

        PAYLOAD_TOO_HEAVY,

        PAYLOAD_TOO_LONG,

        PAYLOAD_TOO_WIDE,

        POWER_SHORT,

        FUEL_SHORT,

        OVER_BUDGET
    }

    public BuildIssue {
        Objects.requireNonNull(kind, "kind");
    }
}
