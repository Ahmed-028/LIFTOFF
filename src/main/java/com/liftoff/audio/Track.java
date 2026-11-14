package com.liftoff.audio;

import static com.liftoff.audio.Synth.Wave.SAW;
import static com.liftoff.audio.Synth.Wave.SINE;
import static com.liftoff.audio.Synth.Wave.SQUARE;
import static com.liftoff.audio.Synth.Wave.TRIANGLE;
import static com.liftoff.audio.Synth.freq;


public enum Track {

    HANGAR,

    ARCADE,

    FINALE;


    private static final int[] C = {48, 60, 64, 67};
    private static final int[] AM = {45, 57, 60, 64};
    private static final int[] F = {41, 57, 60, 65};
    private static final int[] G = {43, 55, 59, 62};
    private static final int[] EM = {40, 55, 59, 64};

    public float[] render() {
        return switch (this) {
            case HANGAR -> hangar();
            case ARCADE -> arcade();
            case FINALE -> finale();
        };
    }

    private static float[] hangar() {
        double beat = 60.0 / 96;
        double bar = beat * 4;
        int[][] chords = {C, AM, F, G, C, AM, F, G, AM, F, C, G, AM, F, G, G};
        var out = Synth.buffer(bar * chords.length);
        for (int b = 0; b < chords.length; b++) {
            int[] chord = chords[b];
            double at = b * bar;
            for (int i = 1; i < 4; i++) {
                Synth.tone(out, at, bar + .05, freq(chord[i]), .05, TRIANGLE, .5, .6);
            }
            Synth.tone(out, at, bar * .5, freq(chord[0]), .13, TRIANGLE, .02, .2);
            Synth.tone(out, at + bar * .5, bar * .5, freq(chord[0] + 7), .1, TRIANGLE, .02, .2);
            int[] arp = {1, 2, 3, 2, 1, 2, 3, 2};
            for (int s = 0; s < 8; s++) {
                Synth.pluck(out, at + s * beat / 2, beat * 1.2, freq(chord[arp[s]] + 12), .06, SINE, 6);
            }
        }

        double[][] tune = {
                {4, 0, 76, 1}, {4, 1, 79, 1}, {4, 2, 81, 2},
                {5, 0, 79, 1}, {5, 1, 76, 1}, {5, 2, 72, 2},
                {6, 0, 74, 1}, {6, 1, 76, 1}, {6, 2, 79, 1}, {6, 3, 76, 1},
                {7, 0, 74, 3}, {7, 3, 72, 1},
                {12, 0, 76, 1}, {12, 1, 81, 1}, {12, 2, 79, 2},
                {13, 0, 76, 1}, {13, 1, 74, 1}, {13, 2, 72, 2},
                {14, 0, 74, 2}, {14, 2, 79, 2},
                {15, 0, 76, 4}};
        for (double[] note : tune) {
            double at = note[0] * bar + note[1] * beat;
            Synth.sweep(out, at, note[3] * beat * .95, freq(note[2]), freq(note[2]), .11, SINE, .03,
                    Math.min(.3, note[3] * beat * .4), .12);
        }
        Synth.echo(out, beat * .75, .28);
        return Synth.normalise(out, .9);
    }

    private static float[] arcade() {
        double beat = 60.0 / 132;
        double bar = beat * 4;
        int[][] chords = {AM, F, C, G, AM, F, C, G, AM, F, C, G, AM, F, EM, G};
        var out = Synth.buffer(bar * chords.length);
        for (int b = 0; b < chords.length; b++) {
            int[] chord = chords[b];
            double at = b * bar;
            for (int q = 0; q < 4; q++) {
                double t = at + q * beat;
                Synth.sweep(out, t, .18, 130, 45, .55, SINE, .002, .12, 0);
                Synth.noise(out, t + beat / 2, .035, .12, .001, .03, .95, .95, b * 8 + q);
                if (q % 2 == 1) {
                    Synth.noise(out, t, .12, .3, .002, .1, .6, .3, 500 + b * 4 + q);
                    Synth.tone(out, t, .08, 185, .12, TRIANGLE, .002, .06);
                }
            }
            for (int e = 0; e < 8; e++) {
                int note = chord[0] + (e % 2 == 1 ? 12 : 0);
                Synth.pluck(out, at + e * beat / 2, beat / 2, freq(note), .16, SQUARE, 9);
            }
            for (int i = 1; i < 4; i++) {
                Synth.pluck(out, at, beat, freq(chord[i]), .05, TRIANGLE, 4);
            }
        }

        int[] riff = {81, 84, 86, 84, 81, 79, 76, 79};
        int[] answer = {76, 79, 81, 84, 86, 88, 86, 84};
        for (int b : new int[] {4, 5, 6, 7, 12, 13, 14, 15}) {
            int[] line = b % 2 == 0 ? riff : answer;
            for (int e = 0; e < 8; e++) {
                if (b >= 14 && e % 4 == 3) {
                    continue;
                }
                Synth.pluck(out, b * bar + e * beat / 2, beat / 2, freq(line[e]), .1, SQUARE, 7);
            }
        }
        Synth.echo(out, beat * .75, .18);
        return Synth.normalise(out, .9);
    }

    private static float[] finale() {
        double beat = 60.0 / 100;
        double bar = beat * 4;
        int[][] chords = {C, F, C, G, AM, F, G, C};
        var out = Synth.buffer(bar * chords.length);
        for (int b = 0; b < chords.length; b++) {
            int[] chord = chords[b];
            double at = b * bar;
            Synth.sweep(out, at, 1.2, 70, 40, .55, SINE, .003, 1.0, 0);
            Synth.noise(out, at, .4, .12, .002, .35, .08, .03, 900 + b);
            for (int i = 1; i < 4; i++) {
                Synth.tone(out, at, bar, freq(chord[i]), .06, TRIANGLE, .15, .4);
            }
            Synth.tone(out, at, bar, freq(chord[0]), .12, TRIANGLE, .02, .3);
        }

        double[][] call = {
                {0, 0, 67, .66}, {0, .66, 67, .34}, {0, 1, 72, 1}, {0, 2, 76, 1}, {0, 3, 79, 1},
                {1, 0, 81, 2}, {1, 2, 77, 2},
                {2, 0, 79, .66}, {2, .66, 79, .34}, {2, 1, 76, 1}, {2, 2, 72, 2},
                {3, 0, 74, 4},
                {4, 0, 76, 1}, {4, 1, 72, 1}, {4, 2, 76, 1}, {4, 3, 81, 1},
                {5, 0, 77, 2}, {5, 2, 81, 2},
                {6, 0, 79, 1}, {6, 1, 81, 1}, {6, 2, 83, 2},
                {7, 0, 84, 4}};
        for (double[] note : call) {
            double at = note[0] * bar + note[1] * beat;
            double length = note[3] * beat * .96;
            Synth.sweep(out, at, length, freq(note[2]), freq(note[2]), .13, SAW, .04, Math.min(.25, length * .4), .15);
            Synth.sweep(out, at, length, freq(note[2] - 12), freq(note[2] - 12), .06, SAW, .04, Math.min(.25, length * .4), 0);
        }
        Synth.echo(out, beat * .5, .22);
        return Synth.normalise(out, .9);
    }
}
