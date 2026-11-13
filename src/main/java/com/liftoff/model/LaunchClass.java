package com.liftoff.model;

/** The class of orbit a published rocket lift capacity applies to. */
public enum LaunchClass {
    /** Low Earth orbit inclined up to about 40 degrees; uses the reference-LEO figure. */
    LOW_INCLINATION,
    /** Near-polar and sun-synchronous orbits; uses the polar figure. */
    POLAR
}
