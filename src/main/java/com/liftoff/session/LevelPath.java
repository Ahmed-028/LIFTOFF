package com.liftoff.session;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;


public final class LevelPath {

    private static final Level[] LEVELS = Level.values();

    private final Predicate<MissionStep> stepUnlocked;
    private final Supplier<MissionStep> currentStep;
    private final EnumSet<Level> gamesFinished = EnumSet.noneOf(Level.class);
    private Level position = Level.BUILD;


    public LevelPath(Predicate<MissionStep> stepUnlocked, Supplier<MissionStep> currentStep) {
        this.stepUnlocked = Objects.requireNonNull(stepUnlocked);
        this.currentStep = Objects.requireNonNull(currentStep);
    }

    public boolean isDone(Level level) {
        return switch (level) {
            case BUILD -> stepUnlocked.test(MissionStep.SUPPLIES);
            case PACK -> stepUnlocked.test(MissionStep.LAUNCH);
            case LIFTOFF -> stepUnlocked.test(MissionStep.REPORT);
            case STAR_MATCH, ASTEROID_DASH -> gamesFinished.contains(level);
        };
    }

    public boolean isOpen(Level level) {
        for (Level earlier : LEVELS) {
            if (earlier == level) {
                return true;
            }
            if (!isDone(earlier)) {
                return false;
            }
        }
        return true;
    }

    public boolean allDone() {
        for (Level level : LEVELS) {
            if (!isDone(level)) {
                return false;
            }
        }
        return true;
    }


    public Level toPlay() {
        for (Level level : LEVELS) {
            if (!isDone(level)) {
                return level;
            }
        }
        return LEVELS[LEVELS.length - 1];
    }


    public List<Level> routeAfter(Level finished) {
        var route = new ArrayList<Level>();
        for (int index = finished.ordinal() + 1; index < LEVELS.length; index++) {
            route.add(LEVELS[index]);
            if (!isDone(LEVELS[index])) {
                return route;
            }
        }
        return List.of();
    }


    public void finishGame(Level game) {
        if (game.kind() != Level.Kind.MINI_GAME) {
            throw new IllegalArgumentException(game + " is not a mini-game");
        }
        requireOpen(game);
        gamesFinished.add(game);
    }


    public Level position() {
        return position;
    }


    public void moveTo(Level level) {
        requireOpen(level);
        position = level;
    }


    public MissionStep entryStep(Level level) {
        if (level.kind() != Level.Kind.MISSION) {
            throw new IllegalArgumentException(level + " has no workflow steps");
        }
        MissionStep current = currentStep.get();
        if (level.steps().contains(current)) {
            return current;
        }
        return level == Level.LIFTOFF && isDone(Level.LIFTOFF) ? MissionStep.REPORT : level.steps().get(0);
    }


    public void reset() {
        gamesFinished.clear();
        position = Level.BUILD;
    }

    private void requireOpen(Level level) {
        if (!isOpen(level)) {
            throw new IllegalStateException("Level " + level + " is locked");
        }
    }
}
