package com.liftoff.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;


class SoundTest {

    @Test
    void everyEffectIsShortCleanAndAudible() {
        for (Sfx sfx : Sfx.values()) {
            float[] samples = sfx.render();
            assertTrue(samples.length > 0 && samples.length <= Synth.RATE * 4, sfx + " length");
            assertClean(sfx.name(), samples);
        }
    }

    @Test
    void everyTrackIsALoopOfReasonableLength() {
        for (Track track : Track.values()) {
            float[] samples = track.render();
            double seconds = samples.length / (double) Synth.RATE;
            assertTrue(seconds > 15 && seconds < 60, track + " lasts " + seconds + " s");
            assertClean(track.name(), samples);
        }
    }

    private static void assertClean(String name, float[] samples) {
        double peak = 0;
        for (float sample : samples) {
            assertTrue(Float.isFinite(sample), name + " has a broken sample");
            peak = Math.max(peak, Math.abs(sample));
        }
        assertTrue(peak > .05, name + " is silent");
        assertTrue(peak <= 1.5, name + " is far too loud: " + peak);
    }
}
