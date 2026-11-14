package com.liftoff.engine.score;

import java.util.List;


public record Report(List<WhyCard> worked, WhyCard brokeFirst, WhyCard tryNext) {

    public Report {
        worked = List.copyOf(worked);
    }
}
