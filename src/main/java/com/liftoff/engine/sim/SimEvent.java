package com.liftoff.engine.sim;

/** An event that happened during a flight, for the Fly screen to play back. */
public record SimEvent(int timeIndex, SimEventType type, String messageKey, SimEffect effect) {
}
