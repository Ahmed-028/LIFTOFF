package com.liftoff.engine.sim;

/** The limits a flight can run into. The report names the first one that broke. */
public enum SimLimit {
    NONE,
    /** The instruments wanted more power than there was, so they ran part of the time. */
    POWER,
    /** More data was ready than the ground contacts could send home. */
    DOWNLINK,
    /** The data recorder was full and data was lost. */
    STORAGE,
    /** The crew's supplies ran out: the mission failed. */
    SUPPLIES
}
