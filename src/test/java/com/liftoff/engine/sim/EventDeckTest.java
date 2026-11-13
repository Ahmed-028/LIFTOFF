package com.liftoff.engine.sim;

import com.liftoff.model.Destination;
import com.liftoff.model.Difficulty;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventDeckTest {

    private static final int STEPS = 2000;

    private final EventDeck deck = SimFixtures.deck();

    private static long drawn(EventDeck deck, Difficulty difficulty, long seed) {
        return IntStream.range(0, STEPS).filter(step -> deck.draw(difficulty, seed, step).isPresent()).count();
    }

    @Test
    void theFixtureHasOneCardOfEachType() {
        assertEquals(List.of(SimEventType.values()), deck.cards().stream().map(EventCard::type).toList());
    }

    @Test
    void cadetNeverDrawsAnEvent() {
        assertEquals(0, drawn(deck, Difficulty.EASY, 7));
    }

    @Test
    void anEmptyDeckNeverDrawsAnEvent() {
        assertEquals(0, drawn(EventDeck.empty(), Difficulty.HARD, 7));
    }

    @Test
    void theSameSeedAndStepAlwaysDrawTheSameCard() {
        for (int step = 0; step < STEPS; step++) {
            assertEquals(deck.draw(Difficulty.HARD, 42, step),
                    deck.draw(Difficulty.HARD, 42, step));
        }
    }

    @Test
    void differentSeedsGiveDifferentDraws() {
        List<Optional<EventCard>> first = IntStream.range(0, STEPS)
                .mapToObj(step -> deck.draw(Difficulty.HARD, 1, step)).toList();
        List<Optional<EventCard>> second = IntStream.range(0, STEPS)
                .mapToObj(step -> deck.draw(Difficulty.HARD, 2, step)).toList();

        assertFalse(first.equals(second));
    }

    @Test
    void harderDifficultiesDrawMoreEvents() {
        long engineer = drawn(deck, Difficulty.MEDIUM, 7);
        long director = drawn(deck, Difficulty.HARD, 7);

        assertTrue(engineer > 0);
        assertTrue(director > engineer, engineer + " vs " + director);
    }

    @Test
    void aDeckCanBeNarrowedToTheEventsOfOneDestination() {
        List<SimEventType> moon = deck.forDestination(Destination.MOON_SURFACE).cards().stream()
                .map(EventCard::type).toList();

        assertEquals(List.of(SimEventType.SOLAR_STORM, SimEventType.LAUNCH_SLIP), moon);
    }

    @Test
    void aMalformedEventsFileIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> EventDeck.parse("[{ \"id\": \"x\", \"typo\": 1 }]"));
    }
}
