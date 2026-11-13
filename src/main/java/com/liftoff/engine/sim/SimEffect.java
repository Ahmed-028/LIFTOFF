package com.liftoff.engine.sim;

/**
 * What an event did to the flight.
 *
 * @param fraction share lost (0 to 1) of whatever the event type hits: science time, a station's
 *                 contact, an instrument's data or solar power; 0 when the event had nothing to hit
 * @param days     how long it lasts; infinite for the rest of the flight
 * @param targetId the instrument or ground station hit, or null
 */
public record SimEffect(double fraction, double days, String targetId) {

    static SimEffect none() {
        return new SimEffect(0, 0, null);
    }
}
