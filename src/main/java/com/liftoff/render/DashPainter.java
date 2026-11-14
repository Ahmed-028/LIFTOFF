package com.liftoff.render;

import com.liftoff.minigame.RunnerGame;
import com.liftoff.session.stub.Mission;
import javafx.scene.image.Image;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineCap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.SplittableRandom;


public final class DashPainter {

    private static final double FOCAL = 11;
    private static final int STAR_COUNT = 140;

    private final Theme.World world;
    private final Mission.Destination destination;
    private final Image planetMap;
    private final double[][] stars = new double[STAR_COUNT][3];
    private final double[][] dust = new double[60][3];

    private final Color rockLight;
    private final Color rockDark;
    private final Color trackNear;
    private final Color trackFar;


    public DashPainter(Mission.Destination destination) {
        this.destination = destination;
        this.world = Theme.World.of(destination);
        var map = DashPainter.class.getResource("/com/liftoff/art/flight/"
                + destination.name().toLowerCase(java.util.Locale.ROOT) + "-map.jpg");
        planetMap = map == null ? null : new Image(map.toExternalForm());
        switch (destination) {
            case EARTH -> {
                rockLight = Color.web("#C4CDD6");
                rockDark = Color.web("#6F7F99");
                trackNear = Theme.NAVY_700;
                trackFar = Theme.NAVY_800;
            }
            case MOON -> {
                rockLight = Color.web("#B5B8C0");
                rockDark = Color.web("#6E717A");
                trackNear = Color.web("#5A5E68");
                trackFar = Color.web("#2C2F37");
            }
            default -> {
                rockLight = Color.web("#C26A42");
                rockDark = Color.web("#6B2E1A");
                trackNear = Color.web("#8E3B22");
                trackFar = Color.web("#4A1A10");
            }
        }
        var dustRandom = new SplittableRandom(23);
        for (double[] grain : dust) {
            grain[0] = dustRandom.nextDouble();
            grain[1] = dustRandom.nextDouble();
            grain[2] = dustRandom.nextDouble(.5, 1.5);
        }
        var random = new SplittableRandom(11);
        for (double[] star : stars) {
            star[0] = random.nextDouble(-1, 1);
            star[1] = random.nextDouble(-1, 1);
            star[2] = random.nextDouble(.1, 1);
        }
    }

    private record View(double w, double h, double horizon, double base, double cx, double half) {
        double scale(double z) {
            return FOCAL / (FOCAL + Math.max(-FOCAL * .8, z));
        }

        double y(double z) {
            return horizon + (base - horizon) * scale(z);
        }

        double x(double lane, double z) {
            return cx + lane * half * 2 / 3 * scale(z);
        }


        double unit(double z) {
            return half * 2 / 3 * scale(z);
        }
    }


    public void paint(GraphicsContext g, double w, double h, RunnerGame game, double time) {
        var view = new View(w, h, h * .30, h - 20, w / 2, Math.min(w * .42, h * .95));
        sky(g, view, game, time);
        track(g, view, game);
        var things = new ArrayList<>(game.things());
        things.sort(Comparator.comparingDouble(RunnerGame.Thing::z).reversed());
        boolean riderDrawn = false;
        for (var thing : things) {
            if (!riderDrawn && thing.z() < 0) {
                rider(g, view, game, time);
                riderDrawn = true;
            }
            if (thing.z() > RunnerGame.SPAWN_AHEAD || thing.z() < -1.2) {
                continue;
            }
            thing(g, view, thing, time);
        }
        if (!riderDrawn) {
            rider(g, view, game, time);
        }
        if (destination == Mission.Destination.MARS) {

            for (double[] grain : dust) {
                double gx = ((grain[0] + time * .08 * grain[2]) % 1) * w;
                double gy = view.horizon() + grain[1] * (h - view.horizon());
                g.setFill(Theme.alpha(Color.web("#E08A5A"), .35));
                g.fillOval(gx, gy, 2.5 * grain[2], 2.5 * grain[2]);
            }
        }
        if (game.safe() && game.hits() > 0) {
            g.setFill(Theme.alpha(Theme.ALERT, .18 * Math.abs(Math.sin(time * 12))));
            g.fillRect(0, 0, w, h);
        }
    }

    private void sky(GraphicsContext g, View v, RunnerGame game, double time) {
        g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Theme.NAVY_950), new Stop(.55, world.bottom), new Stop(1, world.panel)));
        g.fillRect(0, 0, v.w, v.h);

        double travel = game.distance() * .012;
        for (double[] star : stars) {
            double depth = (star[2] - travel) % 1;
            if (depth < 0) {
                depth += 1;
            }
            double reach = 1 / (depth + .08) * .09;
            double x = v.cx + star[0] * v.w * reach;
            double y = v.horizon * .9 + star[1] * v.h * reach * .6;
            double size = (1 - depth) * 2.2 + .4;
            g.setFill(Theme.alpha(Theme.TEXT, .25 + (1 - depth) * .6));
            g.fillOval(x - size / 2, y - size / 2, size, size);
        }

        double radius = 26 + 120 * game.progress();
        double py = v.horizon - radius * .55;
        if (planetMap != null) {
            // Use the NASA map as the destination globe.
            g.save();
            g.beginPath();
            g.arc(v.cx, py, radius, radius, 0, 360);
            g.closePath();
            g.clip();
            double mapWidth = planetMap.getWidth();
            double mapHeight = planetMap.getHeight();
            double from = (mapWidth * .25 + game.distance() * .4) % (mapWidth * .5);
            g.drawImage(planetMap, from, 0, mapWidth * .5, mapHeight, v.cx - radius, py - radius, radius * 2, radius * 2);
            g.setFill(new RadialGradient(0, 0, v.cx - radius * .3, py - radius * .3, radius * 1.3, false,
                    CycleMethod.NO_CYCLE, new Stop(0, Color.TRANSPARENT), new Stop(.75, Color.TRANSPARENT),
                    new Stop(1, Theme.alpha(Theme.NAVY_950, .75))));
            g.fillRect(v.cx - radius, py - radius, radius * 2, radius * 2);
            g.restore();
        } else {
            g.setFill(new RadialGradient(0, 0, v.cx - radius * .3, py - radius * .35, radius * 1.4, false,
                    CycleMethod.NO_CYCLE, new Stop(0, world.accent), new Stop(1, world.raised)));
            g.fillOval(v.cx - radius, py - radius, radius * 2, radius * 2);
        }
        g.setStroke(Theme.alpha(world.accent, .35));
        g.setLineWidth(3);
        g.strokeOval(v.cx - radius - 6, py - radius - 6, radius * 2 + 12, radius * 2 + 12);

        double gateZ = game.finish() - game.distance();
        if (gateZ < RunnerGame.SPAWN_AHEAD) {
            double s = v.unit(gateZ);
            double y = v.y(gateZ);
            double left = v.x(-1.6, gateZ);
            double right = v.x(1.6, gateZ);
            g.setStroke(Theme.ORANGE);
            g.setLineWidth(Math.max(2, s * .12));
            g.strokeLine(left, y, left, y - s * 2.2);
            g.strokeLine(right, y, right, y - s * 2.2);
            g.setStroke(Theme.CYAN);
            g.strokeLine(left, y - s * 2.2, right, y - s * 2.2);
        }
    }

    private void track(GraphicsContext g, View v, RunnerGame game) {
        double far = RunnerGame.SPAWN_AHEAD;
        double near = -1.2;
        g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Theme.alpha(trackFar, .25)), new Stop(1, Theme.alpha(trackNear, .95))));
        g.fillPolygon(new double[] {v.x(-1.5, far), v.x(1.5, far), v.x(1.5, near), v.x(-1.5, near)},
                new double[] {v.y(far), v.y(far), v.y(near), v.y(near)}, 4);

        for (int k = 0; k < 24; k++) {
            double z = k * 5 - game.distance() % 5;
            if (z < near || z > far) {
                continue;
            }
            double fade = 1 - z / far;
            g.setStroke(Theme.alpha(Theme.SHELL_LINE_STRONG, .45 * fade));
            g.setLineWidth(Math.max(1, 3 * v.scale(z)));
            g.strokeLine(v.x(-1.5, z), v.y(z), v.x(1.5, z), v.y(z));
        }
        g.setLineCap(StrokeLineCap.ROUND);
        for (double lane : new double[] {-.5, .5}) {
            g.setStroke(Theme.alpha(Theme.CYAN, .55));
            g.setLineWidth(3);
            g.strokeLine(v.x(lane, far), v.y(far), v.x(lane, near), v.y(near));
        }
        for (double edge : new double[] {-1.5, 1.5}) {
            g.setStroke(Theme.alpha(Theme.ORANGE, .9));
            g.setLineWidth(6);
            g.strokeLine(v.x(edge, far), v.y(far), v.x(edge, near), v.y(near));
        }
    }

    private void thing(GraphicsContext g, View v, RunnerGame.Thing thing, double time) {
        double u = v.unit(thing.z());
        double x = v.x(thing.lane(), thing.z());
        double ground = v.y(thing.z());
        double fade = thing.z() > 60 ? Math.max(0, 1 - (thing.z() - 60) / 30) : 1;
        g.setGlobalAlpha(fade * (thing.done() && thing.kind() != RunnerGame.Kind.STAR ? .35 : 1));
        switch (thing.kind()) {
            case ROCK -> {
                g.setFill(Theme.alpha(Theme.NAVY_950, .5));
                g.fillOval(x - u * .34, ground - u * .07, u * .68, u * .14);
                if (destination == Mission.Destination.EARTH) {

                    g.setFill(rockLight);
                    g.fillRect(x - u * .26, ground - u * .32, u * .52, u * .3);
                    g.setFill(Color.web("#D9AE5E"));
                    g.fillRect(x - u * .1, ground - u * .32, u * .22, u * .3);
                    g.setStroke(Theme.NAVY_950);
                    g.setLineWidth(Math.max(1, u * .03));
                    g.strokeRect(x - u * .26, ground - u * .32, u * .52, u * .3);
                } else {
                    g.setFill(rockLight);
                    g.fillOval(x - u * .3, ground - u * .34, u * .6, u * .36);
                    g.setFill(rockDark);
                    g.fillOval(x - u * .12, ground - u * .26, u * .12, u * .08);
                    g.fillOval(x + u * .06, ground - u * .18, u * .09, u * .06);
                    g.setStroke(Theme.NAVY_950);
                    g.setLineWidth(Math.max(1, u * .03));
                    g.strokeOval(x - u * .3, ground - u * .34, u * .6, u * .36);
                }
            }
            case BEAM -> {
                double top = ground - u * 1.15;
                double bottom = ground - u * .78;
                g.setFill(Theme.SHELL_LINE_STRONG);
                g.fillRect(x - u * .46, top, u * .07, ground - top);
                g.fillRect(x + u * .39, top, u * .07, ground - top);
                int stripes = 6;
                for (int i = 0; i < stripes; i++) {

                    g.setFill(destination == Mission.Destination.EARTH
                            ? (i % 2 == 0 ? Color.web("#2F64CF") : Color.web("#5B8CFF"))
                            : (i % 2 == 0 ? Theme.ORANGE : Theme.HULL_WHITE));
                    double sx = x - u * .46 + i * u * .92 / stripes;
                    g.fillRect(sx, top, u * .92 / stripes + .5, bottom - top);
                }
                g.setStroke(Theme.NAVY_950);
                g.setLineWidth(Math.max(1, u * .025));
                g.strokeRect(x - u * .46, top, u * .92, bottom - top);
            }
            case BOULDER -> {
                double r = u * .44;
                double cy = ground - r;
                g.setFill(Theme.alpha(Theme.NAVY_950, .5));
                g.fillOval(x - r, ground - u * .08, r * 2, u * .16);
                if (destination == Mission.Destination.EARTH) {

                    g.setFill(Color.web("#2F64CF"));
                    g.fillRect(x - r * 1.05, cy - r * .2, r * .55, r * .4);
                    g.fillRect(x + r * .5, cy - r * .2, r * .55, r * .4);
                    g.setFill(Color.web("#D9AE5E"));
                    g.fillRect(x - r * .5, cy - r * .55, r, r * 1.1);
                    g.setStroke(Theme.NAVY_950);
                    g.setLineWidth(Math.max(1.5, u * .04));
                    g.strokeRect(x - r * .5, cy - r * .55, r, r * 1.1);
                    break;
                }
                g.setFill(new RadialGradient(0, 0, x - r * .35, cy - r * .4, r * 1.6, false, CycleMethod.NO_CYCLE,
                        new Stop(0, rockLight), new Stop(1, rockDark)));
                g.fillOval(x - r, cy - r, r * 2, r * 2);
                g.setFill(rockDark.darker());
                g.fillOval(x - r * .45, cy - r * .2, r * .38, r * .28);
                g.fillOval(x + r * .2, cy + r * .25, r * .3, r * .22);
                g.fillOval(x + r * .1, cy - r * .55, r * .22, r * .16);
                g.setStroke(Theme.NAVY_950);
                g.setLineWidth(Math.max(1.5, u * .04));
                g.strokeOval(x - r, cy - r, r * 2, r * 2);
            }
            case STAR -> {
                if (!thing.done()) {
                    star(g, x, ground - u * .38, u * .17, time * 3 + thing.z());
                }
            }
        }
        g.setGlobalAlpha(1);
    }

    private static void star(GraphicsContext g, double x, double y, double r, double spin) {
        double[] xs = new double[10];
        double[] ys = new double[10];
        double squash = .55 + .45 * Math.abs(Math.cos(spin));
        for (int i = 0; i < 10; i++) {
            double rr = i % 2 == 0 ? r : r * .45;
            double a = Math.PI / 5 * i - Math.PI / 2;
            xs[i] = x + Math.cos(a) * rr * squash;
            ys[i] = y + Math.sin(a) * rr;
        }
        g.setFill(Theme.alpha(Theme.AMBER, .3));
        g.fillOval(x - r * 1.5, y - r * 1.5, r * 3, r * 3);
        g.setFill(Theme.AMBER);
        g.fillPolygon(xs, ys, 10);
        g.setStroke(Theme.NAVY_950);
        g.setLineWidth(Math.max(1, r * .12));
        g.strokePolygon(xs, ys, 10);
    }


    private void rider(GraphicsContext g, View v, RunnerGame game, double time) {
        double z = 0;
        double u = v.unit(z) * .78;
        double x = v.x(game.laneX(), z);
        double ground = v.y(z);
        double lift = game.height() * u * .55;
        double hover = Math.sin(time * 6) * u * .02;
        double y = ground - u * .16 - lift + hover;
        boolean blink = game.safe() && game.hits() > 0 && ((int) (time * 10)) % 2 == 0;
        g.setGlobalAlpha(blink ? .45 : 1);


        double shadow = Math.max(.35, 1 - lift / (u * .9));
        g.setFill(Theme.alpha(Theme.NAVY_950, .55));
        g.fillOval(x - u * .36 * shadow, ground - u * .05, u * .72 * shadow, u * .1);


        g.setFill(Theme.alpha(Theme.CYAN, .35));
        g.fillOval(x - u * .4, y - u * .02, u * .8, u * .14);
        g.setFill(Theme.NAVY_700);
        g.fillRoundRect(x - u * .38, y - u * .08, u * .76, u * .12, u * .12, u * .12);
        g.setFill(Theme.ORANGE);
        g.fillRoundRect(x - u * .3, y - u * .065, u * .6, u * .04, u * .04, u * .04);


        double squash = game.ducking() ? .6 : 1;
        double legH = u * .2 * squash;
        double bodyH = u * .26 * squash;
        double bodyW = u * .26;
        double legTop = y - u * .08 - legH;
        double bodyTop = legTop - bodyH;
        double step = Math.sin(time * 9) * u * .02;
        g.setStroke(Theme.NAVY_950);
        g.setLineWidth(Math.max(1, u * .012));

        for (int side : new int[] {-1, 1}) {
            double lx = x + side * bodyW * .25 - u * .045;
            g.setFill(Color.web("#DCE8F7"));
            g.fillRect(lx, legTop + side * step * .3, u * .09, legH);
            g.strokeRect(lx, legTop + side * step * .3, u * .09, legH);
            g.setFill(Theme.NAVY_700);
            g.fillRect(lx - u * .005, legTop + legH - u * .04, u * .1, u * .05);
        }

        double wave = Math.sin(time * 5) * u * .03;
        for (int side : new int[] {-1, 1}) {
            double ax = side < 0 ? x - bodyW / 2 - u * .08 : x + bodyW / 2;
            double ay = bodyTop + u * .02 + side * wave;
            g.setFill(Theme.HULL_WHITE);
            g.fillRect(ax, ay, u * .08, bodyH * .8);
            g.strokeRect(ax, ay, u * .08, bodyH * .8);
            g.setFill(Theme.ORANGE);
            g.fillRect(ax, ay + bodyH * .62, u * .08, u * .02);
        }

        g.setFill(Theme.HULL_WHITE);
        g.fillRect(x - bodyW / 2, bodyTop, bodyW, bodyH);
        g.strokeRect(x - bodyW / 2, bodyTop, bodyW, bodyH);
        g.setFill(Theme.ORANGE);
        g.fillRect(x - bodyW / 2, bodyTop + bodyH * .82, bodyW, bodyH * .12);
        g.fillRect(x - bodyW * .34, bodyTop + bodyH * .1, bodyW * .68, bodyH * .62);
        g.strokeRect(x - bodyW * .34, bodyTop + bodyH * .1, bodyW * .68, bodyH * .62);

        double head = u * .24;
        double headY = bodyTop - head - u * .01;
        g.setFill(Color.web("#F6C9A0"));
        g.fillRect(x - head / 2 - u * .02, headY + head * .4, u * .02, head * .25);
        g.fillRect(x + head / 2, headY + head * .4, u * .02, head * .25);
        g.setFill(Color.web("#5A3A2A"));
        g.fillRect(x - head / 2, headY, head, head);
        g.strokeRect(x - head / 2, headY, head, head);
        g.setFill(Theme.ORANGE);
        g.fillRect(x - head / 2 - u * .01, headY + head * .3, head + u * .02, head * .14);
        g.setGlobalAlpha(1);
    }
}
