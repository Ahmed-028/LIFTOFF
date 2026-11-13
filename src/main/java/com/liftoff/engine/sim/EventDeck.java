package com.liftoff.engine.sim;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liftoff.session.stub.Mission.Destination;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.SplittableRandom;

/** The random events a flight can draw. The same difficulty, seed and step always draw the same card. */
public final class EventDeck {

    /** Where Member 1's events file will live. Until it exists the bundled deck is empty: no events. */
    static final String CATALOG_RESOURCE = "/com/liftoff/catalog/events.json";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES);

    /** Spreads consecutive steps over unrelated random streams. A bit-mixing constant, not data. */
    private static final long STEP_STREAM = 0x9E3779B97F4A7C15L;

    private final List<EventCard> cards;

    public EventDeck(List<EventCard> cards) {
        this.cards = List.copyOf(cards);
    }

    public static EventDeck empty() {
        return new EventDeck(List.of());
    }

    /** Reads a JSON array of event cards. Unknown or missing fields are an error. */
    public static EventDeck parse(String json) {
        try {
            return new EventDeck(MAPPER.readValue(json,
                    MAPPER.getTypeFactory().constructCollectionType(List.class, EventCard.class)));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot read events: " + e.getOriginalMessage(), e);
        }
    }

    /** The deck bundled with the game; empty while events.json is not in the catalog. */
    public static EventDeck fromClasspath() {
        try (InputStream in = EventDeck.class.getResourceAsStream(CATALOG_RESOURCE)) {
            return in == null ? empty() : parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<EventCard> cards() {
        return cards;
    }

    /** Only the cards that can happen at the destination. */
    public EventDeck forDestination(Destination destination) {
        return new EventDeck(cards.stream().filter(card -> card.destinations().contains(destination)).toList());
    }

    /** At most one event per step; how often is set by {@link SimRules#eventChancePerStep}. */
    public Optional<EventCard> draw(Difficulty difficulty, long seed, int stepIndex) {
        double chance = SimRules.eventChancePerStep(difficulty);
        if (cards.isEmpty() || chance <= 0) {
            return Optional.empty();
        }
        SplittableRandom random = randomFor(seed, stepIndex);
        if (random.nextDouble() >= chance) {
            return Optional.empty();
        }
        return Optional.of(cards.get(random.nextInt(cards.size())));
    }

    /** A repeatable random stream for one step of one seeded run. */
    static SplittableRandom randomFor(long seed, int stepIndex) {
        return new SplittableRandom(seed + STEP_STREAM * (stepIndex + 1L));
    }
}
