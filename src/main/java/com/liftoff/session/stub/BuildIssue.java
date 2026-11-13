package com.liftoff.session.stub;

/** TODO(Member 2): stub of engine/BuildIssue — one warning. */
public record BuildIssue(Kind kind, ModuleSlot slot, double needed, double available) {

    public enum Kind { SLOT_EMPTY, PAYLOAD_TOO_HEAVY, PAYLOAD_TOO_LONG, PAYLOAD_TOO_WIDE,
                       POWER_SHORT, FUEL_SHORT, OVER_BUDGET, SUPPLIES_SHORT }
}
