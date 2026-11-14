package com.liftoff.session;

import java.util.List;


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


    public List<MissionStep> steps() {
        return steps;
    }


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
