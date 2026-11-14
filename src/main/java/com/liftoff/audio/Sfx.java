package com.liftoff.audio;

import static com.liftoff.audio.Synth.Wave.SINE;
import static com.liftoff.audio.Synth.Wave.SQUARE;
import static com.liftoff.audio.Synth.Wave.TRIANGLE;
import static com.liftoff.audio.Synth.freq;


public enum Sfx {
    CLICK, GO, PICK, UNPICK, SNAP, ERROR, WHOOSH, ARRIVE, BEEP, BEEP_HIGH, LIFTOFF,
    SPARKLE, SPARKLE_2, SPARKLE_3, STAR, HIT, WIN, LOSE, POP, CHEER, TYPE, STARTUP;


    public float[] render() {
        return Synth.limit(raw(), .9);
    }

    private float[] raw() {
        return switch (this) {
            case CLICK -> {
                var out = Synth.buffer(.06);
                Synth.pluck(out, 0, .06, 1250, .5, SINE, 70);
                yield out;
            }
            case GO -> {
                var out = Synth.buffer(.24);
                Synth.pluck(out, 0, .1, freq(76), .35, SQUARE, 25);
                Synth.pluck(out, .07, .17, freq(83), .35, SQUARE, 18);
                yield out;
            }
            case PICK -> {
                var out = Synth.buffer(.22);
                Synth.pluck(out, 0, .1, freq(79), .45, SINE, 22);
                Synth.pluck(out, .06, .16, freq(86), .45, SINE, 18);
                yield out;
            }
            case UNPICK -> {
                var out = Synth.buffer(.22);
                Synth.pluck(out, 0, .1, freq(81), .4, SINE, 22);
                Synth.pluck(out, .06, .16, freq(74), .4, SINE, 18);
                yield out;
            }
            case SNAP -> {
                var out = Synth.buffer(.2);
                Synth.noise(out, 0, .025, .7, 0, .02, 1, 1, 3);
                Synth.pluck(out, 0, .18, 196, .6, TRIANGLE, 24);
                Synth.pluck(out, .02, .1, 1318, .2, SINE, 40);
                yield out;
            }
            case ERROR -> {
                var out = Synth.buffer(.3);
                Synth.tone(out, 0, .12, 220, .3, SQUARE, .005, .03);
                Synth.tone(out, .14, .14, 185, .3, SQUARE, .005, .05);
                yield out;
            }
            case WHOOSH -> {
                var out = Synth.buffer(.8);
                Synth.noise(out, 0, .8, .9, .3, .45, .04, .45, 5);
                yield out;
            }
            case ARRIVE -> {
                var out = Synth.buffer(.9);
                int[] notes = {72, 76, 79, 84};
                for (int i = 0; i < notes.length; i++) {
                    Synth.pluck(out, i * .08, .5, freq(notes[i]), .35, SINE, 7);
                }
                Synth.echo(out, .14, .3);
                yield out;
            }
            case BEEP -> {
                var out = Synth.buffer(.18);
                Synth.tone(out, 0, .16, 880, .4, SINE, .005, .04);
                yield out;
            }
            case BEEP_HIGH -> {
                var out = Synth.buffer(.45);
                Synth.tone(out, 0, .42, 1320, .4, SINE, .005, .2);
                yield out;
            }
            case LIFTOFF -> {
                var out = Synth.buffer(3.2);
                Synth.noise(out, 0, 3.2, 1.6, .5, 2.0, .015, .06, 9);
                Synth.sweep(out, 0, 3.2, 48, 70, .35, SINE, .4, 2.0, 0);
                Synth.noise(out, .2, 2.4, .25, .3, 1.6, .3, .1, 11);
                yield out;
            }
            case SPARKLE, SPARKLE_2, SPARKLE_3 -> {
                int lift = (ordinal() - SPARKLE.ordinal()) * 3;
                var out = Synth.buffer(.45);
                int[] notes = {88, 91, 95};
                for (int i = 0; i < notes.length; i++) {
                    Synth.pluck(out, i * .045, .3, freq(notes[i] + lift), .25, SINE, 14);
                }
                yield out;
            }
            case STAR -> {
                var out = Synth.buffer(.2);
                Synth.pluck(out, 0, .15, freq(91), .3, SINE, 20);
                Synth.pluck(out, .05, .15, freq(96), .3, SINE, 20);
                yield out;
            }
            case HIT -> {
                var out = Synth.buffer(.35);
                Synth.noise(out, 0, .2, 1.2, 0, .15, .2, .05, 13);
                Synth.sweep(out, 0, .3, 140, 50, .6, SINE, .002, .2, 0);
                yield out;
            }
            case WIN -> {
                var out = Synth.buffer(1.3);
                int[] notes = {72, 76, 79};
                for (int i = 0; i < notes.length; i++) {
                    Synth.tone(out, i * .12, .14, freq(notes[i]), .25, SQUARE, .01, .04);
                }
                Synth.sweep(out, .36, .8, freq(84), freq(84), .28, SQUARE, .01, .4, .25);
                Synth.tone(out, .36, .8, freq(76), .12, TRIANGLE, .02, .4);
                Synth.tone(out, .36, .8, freq(79), .12, TRIANGLE, .02, .4);
                yield out;
            }
            case LOSE -> {
                var out = Synth.buffer(1.0);
                int[] notes = {67, 66, 65};
                for (int i = 0; i < notes.length; i++) {
                    Synth.tone(out, i * .2, .19, freq(notes[i]), .3, TRIANGLE, .01, .05);
                }
                Synth.sweep(out, .6, .4, freq(64), freq(62), .3, TRIANGLE, .01, .2, .3);
                yield out;
            }
            case POP -> {
                var out = Synth.buffer(.12);
                Synth.sweep(out, 0, .1, 420, 950, .5, SINE, .003, .05, 0);
                yield out;
            }
            case CHEER -> {
                var out = Synth.buffer(3.0);
                Synth.noise(out, 0, 3.0, .35, .4, 1.4, .5, .4, 15);
                var random = new java.util.SplittableRandom(17);
                for (int i = 0; i < 260; i++) {
                    double at = random.nextDouble(0, 2.7);
                    double fade = 1 - at / 3.2;
                    Synth.noise(out, at, .03, .5 * fade, .002, .02, .9, .9, 100 + i);
                }
                yield out;
            }
            case TYPE -> {
                var out = Synth.buffer(.7);
                for (int i = 0; i < 7; i++) {
                    Synth.noise(out, i * .09 + (i % 2) * .02, .02, .5, .001, .015, .95, .95, 40 + i);
                }
                yield out;
            }
            case STARTUP -> {
                var out = Synth.buffer(1.4);
                int[] notes = {64, 68, 71, 76};
                for (int i = 0; i < notes.length; i++) {
                    Synth.pluck(out, i * .09, 1.0, freq(notes[i]), .25, SINE, 4);
                }
                Synth.tone(out, .3, 1.0, freq(52), .12, TRIANGLE, .2, .6);
                Synth.echo(out, .18, .25);
                yield out;
            }
        };
    }
}
