package com.liftoff.ui;

import com.liftoff.session.MissionFlow;
import com.liftoff.session.MissionStep;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StepperBarTest {
    @Test
    void marksTheCurrentStepAndDistinguishesLockedFromAvailableSteps() {
        var current = MissionStep.SHIP;
        assertEquals(StepperBar.State.CURRENT, StepperBar.state(current, current, Optional.empty()));
        assertEquals(StepperBar.State.OPEN, StepperBar.state(MissionStep.BRIEFING, current, Optional.empty()));
        assertEquals(StepperBar.State.LOCKED, StepperBar.state(MissionStep.SUPPLIES, current,
                Optional.of(MissionFlow.SHIP_INCOMPLETE)));
        assertEquals(StepperBar.State.OPEN, StepperBar.state(MissionStep.SUPPLIES, current, Optional.empty()));
    }
}
