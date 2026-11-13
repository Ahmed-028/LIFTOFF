package com.liftoff.model;

/** How a catalog number was obtained. Shown to players on the Sources screen. */
public enum ValueBasis {
    /** Copied directly from the cited source. */
    PUBLISHED,
    /** Computed from published numbers; the arithmetic is in docs/data-sources.md. */
    DERIVED,
    /** Our own estimate, not NASA data. Shown with an "approximately" sign in the game. */
    GAME_ESTIMATE
}
