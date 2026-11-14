package com.liftoff.engine.sim;

// Every value here is a game parameter, not NASA data.
public final class SimRules {


    public static final double DAYS_PER_MONTH = 365.25 / 12;


    public static final double SAFE_MODE_DAYS = 3;


    public static final double STATION_OUTAGE_DAYS = 2;


    public static final double INSTRUMENT_FAULT_DATA_LOST_FRACTION = 0.25;


    public static final int LAUNCH_SLIP_MONTHS = 2;


    public static final double DUST_STORM_DAYS = 30;


    public static final double DUST_SOLAR_POWER_LOST_FRACTION = 0.5;


    public static final double EVENT_CHANCE_ENGINEER = 0.10;


    public static final double EVENT_CHANCE_MISSION_DIRECTOR = 0.25;

    private SimRules() {
    }


    public static double eventChancePerStep(Difficulty difficulty) {
        return switch (difficulty) {
            case CADET -> 0;
            case ENGINEER -> EVENT_CHANCE_ENGINEER;
            case MISSION_DIRECTOR -> EVENT_CHANCE_MISSION_DIRECTOR;
        };
    }
}
