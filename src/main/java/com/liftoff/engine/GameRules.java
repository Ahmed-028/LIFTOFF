package com.liftoff.engine;

/** Tunable rules that are not catalog data. Each says where it comes from. */
public final class GameRules {

    /** A resource is TIGHT from this fraction of its limit: less than 15% margin left. Teaching device. */
    public static final double TIGHT_FRACTION = 0.85;

    /** Lowest angle above the horizon at which a ground station can talk to the spacecraft. Common planning value. */
    public static final double MIN_ELEVATION_DEG = 5.0;

    /** Months to put the spacecraft together and test it, before counting instruments. Game estimate. */
    public static final int INTEGRATION_BASE_MONTHS = 6;

    /** Extra integration and test months for each instrument. Game estimate. */
    public static final int INTEGRATION_MONTHS_PER_INSTRUMENT = 2;

    private GameRules() {
    }
}
