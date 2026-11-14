package com.liftoff.minigame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Asteroid Dash, the second mini-game: ride a hover-board down a three-lane space track to the
 * finish. Low rocks are jumped, high beams are ducked under, big boulders block a whole lane and
 * stars are collected. Every row of obstacles leaves at least one lane clear. Three shields;
 * a hit costs one and gives a moment of safety. Just for fun: nothing here touches the mission.
 *
 * <p>Distances are in game metres along the track, ahead of the rider (rider at 0).
 */
public final class RunnerGame {

    public enum Kind { ROCK, BEAM, BOULDER, STAR }

    /** Something on the track, {@code z} metres ahead of the rider. */
    public static final class Thing {
        private final Kind kind;
        private final int lane;
        private double z;
        private boolean done;

        Thing(Kind kind, int lane, double z) {
            this.kind = kind;
            this.lane = lane;
            this.z = z;
        }

        public Kind kind() {
            return kind;
        }

        public int lane() {
            return lane;
        }

        public double z() {
            return z;
        }

        /** Collected (a star) or already hit. */
        public boolean done() {
            return done;
        }
    }

    public static final int SHIELDS = 3;
    public static final double SPAWN_AHEAD = 90;
    static final double JUMP_TIME = .8;
    static final double JUMP_HEIGHT = 1.5;
    static final double DUCK_TIME = .75;
    static final double SAFE_TIME = 1.3;
    static final double LANE_SPEED = 9;
    static final double HIT_RANGE = .9;

    private final SplittableRandom random;
    private final Challenge challenge;
    private final double finish;
    private final double startSpeed;
    private final double topSpeed;
    private final List<Thing> things = new ArrayList<>();
    private double distance;
    private double speed;
    private int lane;
    private double laneX;
    private double jumpClock;
    private double duckClock;
    private double safeClock;
    private int shields = SHIELDS;
    private int stars;
    private int hits;
    private double nextRow;

    /** A medium ride, for tests. */
    RunnerGame(long seed, boolean calm) {
        this(seed, Challenge.MEDIUM, calm);
    }

    /** @param calm slower and gentler, for calm mode */
    public RunnerGame(long seed, Challenge challenge, boolean calm) {
        this.random = new SplittableRandom(seed);
        this.challenge = challenge;
        this.finish = challenge.dashFinish();
        double gentle = calm ? .7 : 1;
        this.startSpeed = challenge.dashStart() * gentle;
        this.topSpeed = challenge.dashTop() * gentle;
        this.speed = startSpeed;
        this.nextRow = 30;
        while (nextRow < SPAWN_AHEAD) {
            spawnRow(nextRow);
        }
    }

    public void left() {
        lane = Math.max(-1, lane - 1);
    }

    public void right() {
        lane = Math.min(1, lane + 1);
    }

    public void jump() {
        if (!airborne() && duckClock <= 0) {
            jumpClock = JUMP_TIME;
        }
    }

    public void duck() {
        if (!airborne()) {
            duckClock = DUCK_TIME;
        }
    }

    /** Moves the game on by the given seconds. Does nothing once the run is over. */
    public void update(double seconds) {
        if (over() || seconds <= 0) {
            return;
        }
        double step = Math.min(seconds, .05);
        double travelled = speed * step;
        distance = Math.min(finish, distance + travelled);
        speed = Math.min(topSpeed, startSpeed + (topSpeed - startSpeed) * distance / finish);
        double laneStep = LANE_SPEED * step;
        laneX = laneX < lane ? Math.min(lane, laneX + laneStep) : Math.max(lane, laneX - laneStep);
        jumpClock = Math.max(0, jumpClock - step);
        duckClock = Math.max(0, duckClock - step);
        safeClock = Math.max(0, safeClock - step);

        for (Thing thing : things) {
            thing.z -= travelled;
            if (!thing.done && Math.abs(thing.z) < HIT_RANGE && thing.lane == currentLane()) {
                touch(thing);
            }
        }
        things.removeIf(thing -> thing.z < -6);
        nextRow -= travelled;
        while (nextRow < SPAWN_AHEAD && distance + nextRow < finish - 20) {
            spawnRow(nextRow);
        }
        if (seconds > step) {
            update(seconds - step);
        }
    }

    private void touch(Thing thing) {
        if (thing.kind == Kind.STAR) {
            thing.done = true;
            stars++;
            return;
        }
        boolean avoided = (thing.kind == Kind.ROCK && height() > .55) || (thing.kind == Kind.BEAM && ducking());
        if (avoided || safeClock > 0) {
            return;
        }
        thing.done = true;
        shields--;
        hits++;
        safeClock = SAFE_TIME;
    }

    private void spawnRow(double z) {
        int free = random.nextInt(3) - 1;
        boolean narrow = random.nextDouble() >= challenge.dashWideRows();
        for (int l = -1; l <= 1; l++) {
            if (l == free) {
                continue;
            }
            if (narrow && l == other(free)) {
                continue;
            }
            Kind kind = switch (random.nextInt(4)) {
                case 0 -> Kind.BOULDER;
                case 1 -> Kind.BEAM;
                default -> Kind.ROCK;
            };
            things.add(new Thing(kind, l, z));
        }
        if (random.nextInt(3) > 0) {
            for (int k = 0; k < 4; k++) {
                things.add(new Thing(Kind.STAR, free, z - 6 + k * 3));
            }
        }
        nextRow = z + challenge.dashGap() + random.nextDouble(challenge.dashSpread());
    }

    private static int other(int free) {
        return free == 1 ? 0 : free + 1;
    }

    /** The lane the rider counts as being in for collisions. */
    public int currentLane() {
        return (int) Math.round(laneX);
    }

    public double laneX() {
        return laneX;
    }

    public int lane() {
        return lane;
    }

    /** Height of the jump right now, in metres. */
    public double height() {
        if (jumpClock <= 0) {
            return 0;
        }
        double t = 1 - jumpClock / JUMP_TIME;
        return 4 * JUMP_HEIGHT * t * (1 - t);
    }

    public boolean airborne() {
        return jumpClock > 0;
    }

    public boolean ducking() {
        return duckClock > 0;
    }

    /** True for a moment after a hit, while the rider cannot be hit again. */
    public boolean safe() {
        return safeClock > 0;
    }

    public double distance() {
        return distance;
    }

    public double progress() {
        return distance / finish;
    }

    public double speed() {
        return speed;
    }

    /** Length of the ride in game metres. */
    public double finish() {
        return finish;
    }

    public Challenge challenge() {
        return challenge;
    }

    public int shields() {
        return shields;
    }

    public int stars() {
        return stars;
    }

    public int hits() {
        return hits;
    }

    public boolean finished() {
        return distance >= finish && shields > 0;
    }

    public boolean crashed() {
        return shields <= 0;
    }

    public boolean over() {
        return finished() || crashed();
    }

    public List<Thing> things() {
        return Collections.unmodifiableList(things);
    }

    /** Puts a thing on the track, for tests. */
    void place(Kind kind, int lane, double z) {
        things.add(new Thing(kind, lane, z));
    }

    /** Removes everything from the track, for tests. */
    void clearTrack() {
        things.clear();
        nextRow = Double.MAX_VALUE / 4;
    }
}
