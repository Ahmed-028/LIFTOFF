package com.liftoff.model;

import java.util.List;

/** Where a mission goes. Decides which rocket capacity, airframes and rules apply. */
public enum Destination {
    /** Low Earth orbit: Farm Watch and the extra Earth missions. */
    EARTH_ORBIT(List.of(LaunchClass.LOW_INCLINATION, LaunchClass.POLAR)),
    /** The lunar surface and back home: Moon Landing. */
    MOON_SURFACE(List.of(LaunchClass.TRANS_LUNAR)),
    /** The surface of Mars: Mars Sample Drones. */
    MARS_SURFACE(List.of(LaunchClass.TRANS_MARS));

    private final List<LaunchClass> launchClasses;

    Destination(List<LaunchClass> launchClasses) {
        this.launchClasses = launchClasses;
    }

    /** The rocket capacities that can matter for this destination. */
    public List<LaunchClass> launchClasses() {
        return launchClasses;
    }

    /**
     * The rocket capacity this trip uses: in Earth orbit the chosen orbit's, elsewhere the transfer
     * toward the destination. Null in Earth orbit while no orbit is chosen.
     */
    public LaunchClass launchClass(Orbit orbit) {
        if (this != EARTH_ORBIT) {
            return launchClasses.getFirst();
        }
        return orbit == null ? null : orbit.launchClass();
    }
}
