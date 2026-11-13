package com.liftoff.model;

/** Where a mission goes. Decides which rocket capacity, airframes and rules apply. */
public enum Destination {
    /** Low Earth orbit: Farm Watch and the extra Earth missions. */
    EARTH_ORBIT,
    /** The lunar surface and back home: Moon Landing. */
    MOON_SURFACE,
    /** The surface of Mars: Mars Sample Drones. */
    MARS_SURFACE
}
