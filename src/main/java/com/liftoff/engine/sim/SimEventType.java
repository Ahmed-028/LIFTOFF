package com.liftoff.engine.sim;

/** The kinds of random event a flight can meet (starting list in document 06, section 5). */
public enum SimEventType {
    /** The spacecraft goes into safe mode: no science for a few days. */
    SOLAR_STORM,
    /** One ground station cannot be used for a few days. */
    STATION_OUTAGE,
    /** One instrument returns less data for the rest of the flight. */
    INSTRUMENT_FAULT,
    /** The launch is late. */
    LAUNCH_SLIP,
    /** Mars dust dims solar arrays for a while; a nuclear power source is not affected. */
    DUST_STORM
}
