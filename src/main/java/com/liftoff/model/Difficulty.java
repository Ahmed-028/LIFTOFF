package com.liftoff.model;

/** How much leeway a mission gives the player, in ladder order (doc 05 §2). */
public enum Difficulty {
    /** Wide margins: many builds close. Mission 1, Farm Watch. */
    EASY,
    /** Fuel dominates; crew supplies add mass every day. Mission 2, Moon Landing. */
    MEDIUM,
    /** Landing mass capped, power scarce, comms slow. Mission 3, Mars Sample Drones. */
    HARD
}
