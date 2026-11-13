package com.liftoff.engine.sim;

import com.liftoff.model.Destination;
import com.liftoff.model.SourceRef;

import java.util.List;

/**
 * One entry of events.json (owned by Member 1).
 *
 * @param destinations where the event can happen
 * @param realExample  the real occurrence the event is modelled on; written by Member 1 from the source
 * @param source       where that example is documented
 */
public record EventCard(String id, SimEventType type, String messageKey, List<Destination> destinations,
                        String realExample, SourceRef source) {

    public EventCard {
        destinations = List.copyOf(destinations);
    }
}
