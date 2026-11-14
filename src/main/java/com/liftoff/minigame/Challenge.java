package com.liftoff.minigame;

/**
 * How hard the mini-games are. Each mission picks the tier that matches its own difficulty, so
 * the games grow harder from Farm Watch to the Moon to Mars. Game settings only: these numbers
 * are made up for play and are not science values.
 *
 * @param matchGoal     Star Match: tiles to collect
 * @param matchMoves    Star Match: moves allowed
 * @param matchKinds    Star Match: kinds of tile (more kinds make lines harder to find)
 * @param dashFinish    Asteroid Dash: length of the ride in game metres
 * @param dashStart     Asteroid Dash: starting speed, game metres per second
 * @param dashTop       Asteroid Dash: top speed near the finish
 * @param dashGap       Asteroid Dash: shortest gap between rows of obstacles
 * @param dashSpread    Asteroid Dash: extra random gap added to each row
 * @param dashWideRows  Asteroid Dash: chance (0 to 1) that a row blocks two lanes, not one
 */
public record Challenge(String name, int stars, int matchGoal, int matchMoves, int matchKinds,
        double dashFinish, double dashStart, double dashTop, double dashGap, double dashSpread,
        double dashWideRows) {

    public static final Challenge EASY = new Challenge("EASY", 1, 40, 22, 5, 650, 13, 21, 22, 10, .45);
    public static final Challenge MEDIUM = new Challenge("MEDIUM", 2, 60, 20, 6, 900, 16, 27, 16, 10, .7);
    public static final Challenge HARD = new Challenge("HARD", 3, 80, 18, 6, 1150, 19, 32, 13, 8, .85);

    /** The tier for a mission difficulty: 0 is the easiest. */
    public static Challenge forTier(int tier) {
        return switch (Math.max(0, Math.min(2, tier))) {
            case 0 -> EASY;
            case 1 -> MEDIUM;
            default -> HARD;
        };
    }
}
