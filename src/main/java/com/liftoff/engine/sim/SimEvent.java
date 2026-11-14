package com.liftoff.engine.sim;


public record SimEvent(int timeIndex, SimEventType type, String messageKey, SimEffect effect) {
}
