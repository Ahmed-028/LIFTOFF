package com.liftoff.model;

/**
 * The five places on the ship that each hold exactly one part, one per builder tab.
 * Declared in tab order (top to bottom); screens may rely on {@code values()} for that order.
 */
public enum ModuleSlot {
    /** The main body: spaceplane, crew capsule or cruise stage, depending on the mission. Holds the fuel tanks. */
    AIRFRAME,
    /** The engine(s); its specific impulse decides how much fuel the trip needs. */
    THRUSTERS,
    /** Where electricity comes from: solar cells, fuel cells or a radioisotope generator. */
    POWER,
    /** The space inside the body that carries the payload; limits its length, width and mass. */
    CARGO_BAY,
    /** The payload itself: a satellite, a lander with crew, or drones. */
    SATELLITE
}
