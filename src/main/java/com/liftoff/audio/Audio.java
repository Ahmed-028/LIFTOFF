package com.liftoff.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public final class Audio {

    private static final Audio INSTANCE = new Audio();
    private static final int CHUNK = 512;
    private static final double MUSIC_LEVEL = .38;
    private static final double EFFECT_LEVEL = .8;
    private static final double FADE_SECONDS = .9;

    private final Map<Sfx, float[]> effects = new ConcurrentHashMap<>();
    private final Map<Track, float[]> tracks = new ConcurrentHashMap<>();
    private final List<Voice> voices = new CopyOnWriteArrayList<>();
    private final ExecutorService composer = Executors.newSingleThreadExecutor(runnable -> {
        var thread = new Thread(runnable, "liftoff-audio-render");
        thread.setDaemon(true);
        return thread;
    });
    private volatile boolean available = !"off".equalsIgnoreCase(System.getProperty("liftoff.sound"));
    private volatile boolean started;
    private volatile boolean soundOn = true;
    private volatile boolean musicOn = true;
    private volatile Voice music;
    private volatile Voice leaving;
    private volatile Track wanted;

    private Audio() {
    }

    public static Audio get() {
        return INSTANCE;
    }


    public void preload() {
        if (!available) {
            return;
        }
        composer.submit(() -> tracks.computeIfAbsent(Track.HANGAR, Track::render));
        for (Sfx sfx : Sfx.values()) {
            composer.submit(() -> effects.computeIfAbsent(sfx, Sfx::render));
        }
    }

    public boolean soundOn() {
        return soundOn;
    }

    public boolean musicOn() {
        return musicOn;
    }

    public void setSoundOn(boolean on) {
        soundOn = on;
        if (!on) {
            voices.clear();
        }
    }

    public void setMusicOn(boolean on) {
        musicOn = on;
        if (on && wanted != null) {
            var track = wanted;
            wanted = null;
            music(track);
        }
    }


    public void play(Sfx sfx) {
        if (!available || !soundOn) {
            return;
        }
        float[] samples = effects.get(sfx);
        if (samples == null) {
            composer.submit(() -> effects.computeIfAbsent(sfx, Sfx::render));
            return;
        }
        if (ensureStarted()) {
            voices.add(new Voice(null, samples, false, EFFECT_LEVEL));
        }
    }


    public void music(Track track) {
        if (!available || track == wanted) {
            return;
        }
        wanted = track;
        composer.submit(() -> {
            float[] samples = tracks.computeIfAbsent(track, Track::render);
            if (wanted == track && ensureStarted()) {
                var next = new Voice(track, samples, true, 0);
                next.fadeTo(MUSIC_LEVEL);
                var old = music;
                if (old != null) {
                    old.fadeTo(0);
                    leaving = old;
                }
                music = next;
            }
        });
    }

    private synchronized boolean ensureStarted() {
        if (started) {
            return true;
        }
        if (!available) {
            return false;
        }
        try {
            var format = new AudioFormat(Synth.RATE, 16, 1, true, false);
            SourceDataLine line = AudioSystem.getSourceDataLine(format);
            line.open(format, CHUNK * 2 * 8);
            line.start();
            var thread = new Thread(() -> mix(line), "liftoff-audio-mixer");
            thread.setDaemon(true);
            thread.start();
            started = true;
            return true;
        } catch (Exception | LinkageError noSound) {
            available = false;
            return false;
        }
    }

    private void mix(SourceDataLine line) {
        float[] mix = new float[CHUNK];
        byte[] bytes = new byte[CHUNK * 2];
        while (true) {
            java.util.Arrays.fill(mix, 0);
            var current = music;
            if (current != null && musicOn) {
                current.addTo(mix);
            }
            var old = leaving;
            if (old != null) {
                if (musicOn) {
                    old.addTo(mix);
                }
                if (old.silent()) {
                    leaving = null;
                }
            }
            for (Voice voice : voices) {
                voice.addTo(mix);
                if (voice.finished()) {
                    voices.remove(voice);
                }
            }
            for (int i = 0; i < CHUNK; i++) {
                // Limit peaks to prevent clipping.
                double sample = Math.tanh(mix[i] * 1.1);
                int value = (int) Math.round(sample * 32_000);
                bytes[i * 2] = (byte) value;
                bytes[i * 2 + 1] = (byte) (value >> 8);
            }
            line.write(bytes, 0, bytes.length);
        }
    }


    private static final class Voice {
        private final Track track;
        private final float[] samples;
        private final boolean loop;
        private int position;
        private double gain;
        private double target;
        private final double step = 1.0 / (FADE_SECONDS * Synth.RATE);

        Voice(Track track, float[] samples, boolean loop, double gain) {
            this.track = track;
            this.samples = samples;
            this.loop = loop;
            this.gain = gain;
            this.target = gain;
        }

        void fadeTo(double level) {
            target = level;
        }

        void addTo(float[] mix) {
            for (int i = 0; i < mix.length; i++) {
                if (position >= samples.length) {
                    if (!loop) {
                        return;
                    }
                    position = 0;
                }
                if (gain < target) {
                    gain = Math.min(target, gain + step);
                } else if (gain > target) {
                    gain = Math.max(target, gain - step);
                }
                mix[i] += (float) (samples[position++] * gain);
            }
        }

        boolean finished() {
            return !loop && position >= samples.length;
        }

        boolean silent() {
            return gain <= 0 && target <= 0;
        }

        @Override
        public String toString() {
            return track == null ? "effect" : track.name();
        }
    }
}
