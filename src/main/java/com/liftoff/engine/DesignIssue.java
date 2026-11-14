package com.liftoff.engine;

import com.liftoff.model.MeasurementType;

import java.util.Objects;


public record DesignIssue(Kind kind, MeasurementType measurement, double needed, double available) {

    public enum Kind {


        MEASUREMENT_MISSING,


        RESOLUTION_TOO_COARSE,


        ORBIT_MISSES_TARGET
    }

    public DesignIssue {
        Objects.requireNonNull(kind, "kind");
    }
}
