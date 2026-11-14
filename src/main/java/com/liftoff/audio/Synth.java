package com.liftoff.audio;

import java.util.SplittableRandom;


public final class Synth {

    public static final int RATE = 44_100;


    public enum Wave { SINE, TRIANGLE, SQUARE, SAW }

    private Synth() {
    }

    public static float[] buffer(double seconds) {
        return new float[(int) Math.ceil(seconds * RATE)];
    }


    public static double freq(double midi) {
        return 440 * Math.pow(2, (midi - 69) / 12.0);
    }


    public static void tone(float[] out, double start, double duration, double freq, double amp, Wave wave,
            double attack, double release) {
        sweep(out, start, duration, freq, freq, amp, wave, attack, release, 0);
    }


    public static void pluck(float[] out, double start, double duration, double freq, double amp, Wave wave,
            double decay) {
        int from = (int) (start * RATE);
        int count = (int) (duration * RATE);
        double phase = 0;
        for (int i = 0; i < count && from + i < out.length; i++) {
            if (from + i < 0) {
                continue;
            }
            double t = i / (double) RATE;
            double envelope = Math.min(1, t / .004) * Math.exp(-t * decay);
            phase += freq / RATE;
            out[from + i] += (float) (amp * envelope * shape(wave, phase));
        }
    }


    public static void sweep(float[] out, double start, double duration, double from, double to, double amp,
            Wave wave, double attack, double release, double vibrato) {
        int first = (int) (start * RATE);
        int count = (int) (duration * RATE);
        double phase = 0;
        for (int i = 0; i < count && first + i < out.length; i++) {
            if (first + i < 0) {
                continue;
            }
            double t = i / (double) RATE;
            double progress = i / (double) Math.max(1, count);
            double f = from * Math.pow(to / from, progress);
            if (vibrato > 0) {
                f *= Math.pow(2, vibrato * Math.sin(t * Math.TAU * 5.5) * Math.min(1, t * 3) / 12);
            }
            phase += f / RATE;
            out[first + i] += (float) (amp * envelope(t, duration, attack, release) * shape(wave, phase));
        }
    }


    public static void noise(float[] out, double start, double duration, double amp, double attack, double release,
            double brightFrom, double brightTo, long seed) {
        var random = new SplittableRandom(seed);
        int first = (int) (start * RATE);
        int count = (int) (duration * RATE);
        double low = 0;
        for (int i = 0; i < count && first + i < out.length; i++) {
            if (first + i < 0) {
                continue;
            }
            double t = i / (double) RATE;
            double bright = brightFrom + (brightTo - brightFrom) * i / Math.max(1, count);
            low += bright * (random.nextDouble(-1, 1) - low);
            out[first + i] += (float) (amp * envelope(t, duration, attack, release) * low);
        }
    }


    public static void echo(float[] out, double delaySeconds, double feedback) {
        int delay = (int) (delaySeconds * RATE);
        for (int i = delay; i < out.length; i++) {
            out[i] += (float) (out[i - delay] * feedback);
        }
    }


    public static float[] normalise(float[] samples, double peak) {
        float max = 0;
        for (float sample : samples) {
            max = Math.max(max, Math.abs(sample));
        }
        if (max > 0) {
            float scale = (float) (peak / max);
            for (int i = 0; i < samples.length; i++) {
                samples[i] *= scale;
            }
        }
        return samples;
    }


    public static float[] limit(float[] samples, double peak) {
        float max = 0;
        for (float sample : samples) {
            max = Math.max(max, Math.abs(sample));
        }
        return max > peak ? normalise(samples, peak) : samples;
    }

    private static double envelope(double t, double duration, double attack, double release) {
        double in = attack <= 0 ? 1 : Math.min(1, t / attack);
        double left = duration - t;
        double out = release <= 0 ? 1 : Math.min(1, Math.max(0, left / release));
        return in * out;
    }


    private static double shape(Wave wave, double phase) {
        double x = phase - Math.floor(phase);
        return switch (wave) {
            case SINE -> Math.sin(x * Math.TAU);
            case TRIANGLE -> 1 - 4 * Math.abs(x - .5);
            case SQUARE -> Math.sin(x * Math.TAU) + Math.sin(3 * x * Math.TAU) / 3 + Math.sin(5 * x * Math.TAU) / 5;
            case SAW -> Math.sin(x * Math.TAU) - Math.sin(2 * x * Math.TAU) / 2 + Math.sin(3 * x * Math.TAU) / 3
                    - Math.sin(4 * x * Math.TAU) / 4;
        };
    }
}
