package com.liftoff.engine.score;

import java.util.List;

/**
 * The three "why" cards of the report screen.
 *
 * @param worked     what went right
 * @param brokeFirst the earliest thing that went wrong, or the "nothing broke" card
 * @param tryNext    one concrete suggestion that follows from {@code brokeFirst}
 */
public record Report(List<WhyCard> worked, WhyCard brokeFirst, WhyCard tryNext) {

    public Report {
        worked = List.copyOf(worked);
    }
}
