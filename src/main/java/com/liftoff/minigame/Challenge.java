package com.liftoff.minigame;


public record Challenge(String name, int stars, int matchGoal, int matchMoves, int matchKinds,
        double dashFinish, double dashStart, double dashTop, double dashGap, double dashSpread,
        double dashWideRows) {

    public static final Challenge EASY = new Challenge("EASY", 1, 40, 22, 5, 650, 13, 21, 22, 10, .45);
    public static final Challenge MEDIUM = new Challenge("MEDIUM", 2, 60, 20, 6, 900, 16, 27, 16, 10, .7);
    public static final Challenge HARD = new Challenge("HARD", 3, 80, 18, 6, 1150, 19, 32, 13, 8, .85);


    public static Challenge forTier(int tier) {
        return switch (Math.max(0, Math.min(2, tier))) {
            case 0 -> EASY;
            case 1 -> MEDIUM;
            default -> HARD;
        };
    }
}
