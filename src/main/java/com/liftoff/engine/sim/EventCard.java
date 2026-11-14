package com.liftoff.engine.sim;

import com.liftoff.model.SourceRef;
import com.liftoff.session.stub.Mission.Destination;

import java.util.List;


public record EventCard(String id, SimEventType type, String messageKey, List<Destination> destinations,
                        String realExample, SourceRef source) {

    public EventCard {
        destinations = List.copyOf(destinations);
    }
}
