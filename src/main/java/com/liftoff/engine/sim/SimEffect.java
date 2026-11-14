package com.liftoff.engine.sim;


public record SimEffect(double fraction, double days, String targetId) {

    static SimEffect none() {
        return new SimEffect(0, 0, null);
    }
}
