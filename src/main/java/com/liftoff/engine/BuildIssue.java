package com.liftoff.engine;

import com.liftoff.model.ModuleSlot;

import java.util.Objects;

/**
 * One thing wrong with the ship, with the two numbers the warning sentence needs
 * ("Your parts need {needed} W. Your power system gives {available} W.").
 * slot is the tab the warning sits next to; null for OVER_BUDGET and SUPPLIES_SHORT, which concern the whole ship.
 */
public record BuildIssue(Kind kind, ModuleSlot slot, double needed, double available) {

    public enum Kind {
        /** A tab has nothing chosen. needed and available are 0. */
        SLOT_EMPTY,
        /** Cargo heavier than the cargo bay or the airframe can carry (kg). */
        PAYLOAD_TOO_HEAVY,
        /** Satellite longer than the bay (m). */
        PAYLOAD_TOO_LONG,
        /** Satellite wider than the bay (m). */
        PAYLOAD_TOO_WIDE,
        /** Parts draw more power than the power system gives (W). */
        POWER_SHORT,
        /** The trip needs more propellant than the airframe's tanks hold (kg). */
        FUEL_SHORT,
        /** The mission costs more than its budget ($M). */
        OVER_BUDGET,
        /** Fewer crew supplies packed than the crew needs for the trip (kg). Concerns no tab. */
        SUPPLIES_SHORT
    }

    public BuildIssue {
        Objects.requireNonNull(kind, "kind");
    }
}
