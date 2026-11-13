package com.liftoff.engine.sim;

/**
 * How unforgiving a flight is (document 01). Mission 1 is flown at CADET, Mission 2 at ENGINEER,
 * Mission 3 at MISSION_DIRECTOR. TODO(Member 2): Mission will carry this once it is on main.
 */
public enum Difficulty {
    /** No random events. */
    CADET,
    /** Some random events. */
    ENGINEER,
    /** The most random events. */
    MISSION_DIRECTOR
}
