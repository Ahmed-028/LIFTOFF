package com.liftoff.session;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Progress along the road map of the current mission. A mission level is done when the
 * workflow has passed its last step (read from {@link MissionFlow}); a mini-game level is done
 * once played or skipped. A level is open when every level before it is done, so changing the
 * ship (which locks Launch again) closes the levels after packing until the build is
 * confirmed once more.
 */
public final class LevelPath {

    private static final Level[] LEVELS = Level.values();

    private final Predicate<MissionStep> stepUnlocked;
    private final Supplier<MissionStep> currentStep;
    private final EnumSet<Level> gamesFinished = EnumSet.noneOf(Level.class);
    private Level position = Level.BUILD;

    /**
     * @param stepUnlocked answers whether a workflow step is open right now
     * @param currentStep  the workflow step the mission is on
     */
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

    /** The first level that is not done yet: the one the road map suggests. The last level once all are done. */
    public Level toPlay() {
        for (Level level : LEVELS) {
            if (!isDone(level)) {
                return level;
            }
        }
        return LEVELS[LEVELS.length - 1];
    }

    /**
     * The stops the ship flies through after finishing a level: every level after it up to and
     * including the first one that is not done. Empty when all the later levels are done, so
     * replaying an old level needs no travel.
     */
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

    /** @throws IllegalArgumentException if the level is not a mini-game; IllegalStateException if it is locked */
    public void finishGame(Level game) {
        if (game.kind() != Level.Kind.MINI_GAME) {
            throw new IllegalArgumentException(game + " is not a mini-game");
        }
        requireOpen(game);
        gamesFinished.add(game);
    }

    /** Where the ship is parked on the road map. */
    public Level position() {
        return position;
    }

    /** @throws IllegalStateException if the level is locked */
    public void moveTo(Level level) {
        requireOpen(level);
        position = level;
    }

    /**
     * The workflow step to show when a mission level is entered: its current step if the mission
     * is inside it, the report for a finished flight (a flight is flown only once), otherwise the
     * level's first step.
     */
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

    /** Forgets the mini-games and parks the ship at the first level, for a newly chosen mission. */
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
