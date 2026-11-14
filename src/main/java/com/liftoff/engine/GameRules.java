package com.liftoff.engine;

// Game parameters; integration durations are estimates and five degrees is a common cutoff.
public final class GameRules {


    public static final double TIGHT_FRACTION = 0.85;


    public static final double MIN_ELEVATION_DEG = 5.0;


    public static final int INTEGRATION_BASE_MONTHS = 6;


    public static final int INTEGRATION_MONTHS_PER_INSTRUMENT = 2;

    private GameRules() {
    }
}
