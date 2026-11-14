package com.liftoff.model;

public enum ValueBasis {
    // Copied directly from the cited source.
    PUBLISHED,
    // Computed from published numbers; arithmetic is in docs/data-sources.md.
    DERIVED,
    // A game estimate, not NASA data.
    GAME_ESTIMATE
}
