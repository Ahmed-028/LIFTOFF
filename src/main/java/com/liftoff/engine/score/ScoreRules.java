package com.liftoff.engine.score;

/** Tunable rules of the score. Every value here is a game parameter, not NASA data. */
public final class ScoreRules {

    /** Points for science returned (document 01, section 8). Game parameter, not NASA data. */
    public static final double SCIENCE_POINTS = 60;

    /** Points for healthy margins (document 01, section 8). Game parameter, not NASA data. */
    public static final double MARGIN_POINTS = 20;

    /** Points for staying on budget: half of the 20 for "on budget and on time". Game parameter, not NASA data. */
    public static final double BUDGET_POINTS = 10;

    /** Points for finishing on time: the other half. Game parameter, not NASA data. */
    public static final double TIME_POINTS = 10;

    /** Share of a resource's margin credit kept when it is TIGHT. Game parameter, not NASA data. */
    public static final double TIGHT_CREDIT = 0.5;

    /** Lowest total that earns two stars. Game parameter, not NASA data. */
    public static final double TWO_STARS_FROM = 50;

    /** Lowest total that earns three stars. Game parameter, not NASA data. */
    public static final double THREE_STARS_FROM = 80;

    private ScoreRules() {
    }
}
