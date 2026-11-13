package com.liftoff.engine.sim;

/**
 * Tunable rules of the flight simulation. Every value here is a game parameter, not NASA data:
 * they set how the game feels and are open to the balance review.
 */
public final class SimRules {

    /** Length of a month step in days: a 365.25-day year divided by 12. Calendar arithmetic, not NASA data. */
    public static final double DAYS_PER_MONTH = 365.25 / 12;

    /** Days the instruments stay off after a solar storm puts the spacecraft in safe mode. Game parameter, not NASA data. */
    public static final double SAFE_MODE_DAYS = 3;

    /** Days one ground station is unavailable during an outage. Game parameter, not NASA data. */
    public static final double STATION_OUTAGE_DAYS = 2;

    /** Share of an instrument's data lost for the rest of the flight after a fault. Game parameter, not NASA data. */
    public static final double INSTRUMENT_FAULT_DATA_LOST_FRACTION = 0.25;

    /** Months a launch slip adds before the flight starts. Game parameter, not NASA data. */
    public static final int LAUNCH_SLIP_MONTHS = 2;

    /** Days a Mars dust storm dims the solar arrays. Game parameter, not NASA data. */
    public static final double DUST_STORM_DAYS = 30;

    /** Share of solar power lost during a dust storm. Game parameter, not NASA data. */
    public static final double DUST_SOLAR_POWER_LOST_FRACTION = 0.5;

    /** Chance of an event in one step at ENGINEER. Game parameter, not NASA data. */
    public static final double EVENT_CHANCE_ENGINEER = 0.10;

    /** Chance of an event in one step at MISSION_DIRECTOR. Game parameter, not NASA data. */
    public static final double EVENT_CHANCE_MISSION_DIRECTOR = 0.25;

    private SimRules() {
    }

    /** Chance (0 to 1) that a step draws an event. CADET never does. Game parameter, not NASA data. */
    public static double eventChancePerStep(Difficulty difficulty) {
        return switch (difficulty) {
            case CADET -> 0;
            case ENGINEER -> EVENT_CHANCE_ENGINEER;
            case MISSION_DIRECTOR -> EVENT_CHANCE_MISSION_DIRECTOR;
        };
    }
}
