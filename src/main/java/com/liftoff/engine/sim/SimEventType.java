package com.liftoff.engine.sim;


public enum SimEventType {

    SOLAR_STORM,

    STATION_OUTAGE,

    INSTRUMENT_FAULT,

    LAUNCH_SLIP,
    // Dust affects solar arrays, not nuclear power sources.
    DUST_STORM
}
