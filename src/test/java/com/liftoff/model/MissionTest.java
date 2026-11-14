package com.liftoff.model;

import com.liftoff.TestParts;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MissionTest {

    private static Mission mission(String id, Difficulty difficulty, Destination destination,
                                   List<Requirement> required, List<MeasurementType> bonus, SourceRef source) {
        return new Mission(id, "Title", "Question?", "Description", "Based on",
                difficulty, destination, true,
                0, false, required, bonus,
                100, 24, 12,
                1000, 0,
                source);
    }

    @Test
    void difficultiesAreInLadderOrder() {
        assertArrayEquals(new Difficulty[] {Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD},
                Difficulty.values());
    }

    @Test
    void destinationsCoverTheThreeMissions() {
        assertArrayEquals(new Destination[] {
                        Destination.EARTH_ORBIT, Destination.MOON_SURFACE, Destination.MARS_SURFACE},
                Destination.values());
    }

    @Test
    void rejectsMissingId() {
        assertThrows(NullPointerException.class, () -> mission(null, Difficulty.EASY,
                Destination.EARTH_ORBIT, List.of(), List.of(), TestParts.SOURCE));
    }

    @Test
    void rejectsMissingDifficulty() {
        assertThrows(NullPointerException.class, () -> mission("m", null,
                Destination.EARTH_ORBIT, List.of(), List.of(), TestParts.SOURCE));
    }

    @Test
    void rejectsMissingDestination() {
        assertThrows(NullPointerException.class, () -> mission("m", Difficulty.EASY,
                null, List.of(), List.of(), TestParts.SOURCE));
    }

    @Test
    void rejectsMissingSource() {
        assertThrows(NullPointerException.class, () -> mission("m", Difficulty.EASY,
                Destination.EARTH_ORBIT, List.of(), List.of(), null));
    }

    @Test
    void listsCannotBeChangedFromOutside() {
        List<Requirement> required = new ArrayList<>();
        List<MeasurementType> bonus = new ArrayList<>();
        Mission m = mission("m", Difficulty.EASY, Destination.EARTH_ORBIT, required, bonus, TestParts.SOURCE);

        required.add(new Requirement(MeasurementType.values()[0], 30.0));
        bonus.add(MeasurementType.values()[0]);

        assertEquals(List.of(), m.required());
        assertEquals(List.of(), m.bonus());
        assertThrows(UnsupportedOperationException.class, () -> m.bonus().add(MeasurementType.values()[0]));
    }

    @Test
    void rejectsNegativeTripAndBudgetFigures() {

        assertThrows(IllegalArgumentException.class, () -> new Mission("m", "", "", "", "",
                Difficulty.EASY, Destination.EARTH_ORBIT, true, 0, false, List.of(), List.of(),
                -1, 24, 12, 1000, 0, TestParts.SOURCE));
        assertThrows(IllegalArgumentException.class, () -> new Mission("m", "", "", "", "",
                Difficulty.EASY, Destination.EARTH_ORBIT, true, 0, false, List.of(), List.of(),
                100, 24, 12, -1, 0, TestParts.SOURCE));
        assertThrows(IllegalArgumentException.class, () -> new Mission("m", "", "", "", "",
                Difficulty.EASY, Destination.EARTH_ORBIT, true, 0, false, List.of(), List.of(),
                100, 24, 12, 1000, -1, TestParts.SOURCE));
        assertThrows(IllegalArgumentException.class, () -> new Mission("m", "", "", "", "",
                Difficulty.EASY, Destination.EARTH_ORBIT, true, 0, false, List.of(), List.of(),
                100, 24, 12, Double.NaN, 0, TestParts.SOURCE));
    }

    @Test
    void keepsTheTripAndBudgetFigures() {
        Mission m = mission("m", Difficulty.HARD, Destination.MARS_SURFACE, List.of(), List.of(), TestParts.SOURCE);
        assertEquals(1000, m.deltaVRequiredMs());
        assertEquals(100, m.budgetCapMusd());
        assertEquals(Destination.MARS_SURFACE, m.destination());
    }
}
