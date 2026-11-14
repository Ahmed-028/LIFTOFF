package com.liftoff.session;

import java.util.List;

/**
 * The five levels on a mission's road map. Mission levels group the workflow steps (the briefing
 * opens the first level instead of being a level of its own); the two mini-game levels sit
 * between them and never change the design or its outcome.
 */
public enum Level {
    BUILD(Kind.MISSION, List.of(MissionStep.BRIEFING, MissionStep.PLAN, MissionStep.SHIP)),
    STAR_MATCH(Kind.MINI_GAME, List.of()),
    PACK(Kind.MISSION, List.of(MissionStep.SUPPLIES, MissionStep.REVIEW)),
    ASTEROID_DASH(Kind.MINI_GAME, List.of()),
    LIFTOFF(Kind.MISSION, List.of(MissionStep.LAUNCH, MissionStep.FLY, MissionStep.REPORT));

    public enum Kind { MISSION, MINI_GAME }

    private final Kind kind;
    private final List<MissionStep> steps;

    Level(Kind kind, List<MissionStep> steps) {
        this.kind = kind;
        this.steps = steps;
    }

    public Kind kind() {
        return kind;
    }

    /** The workflow steps played inside this level; empty for a mini-game. */
    public List<MissionStep> steps() {
        return steps;
    }

    /** The number shown on the road map, starting at 1. */
    public int number() {
        return ordinal() + 1;
    }

    public static Level of(MissionStep step) {
        for (Level level : values()) {
            if (level.steps.contains(step)) {
                return level;
            }
        }
        throw new IllegalArgumentException("No level holds step " + step);
    }
}
