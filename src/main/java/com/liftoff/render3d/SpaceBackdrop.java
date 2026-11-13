package com.liftoff.render3d;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

import java.util.HashMap;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * The deep-space background: a colourful nebula panorama, far stars that turn with the camera,
 * and near stars that stream past as warp streaks. Everything is projected with the same camera
 * as the 3D scene so the streaks always rush out of the direction of travel.
 */
final class SpaceBackdrop extends Canvas {
    private static final Map<String, Image> NEBULAS = new HashMap<>();
    private static final int FAR = 1300;
    private static final int NEAR = 460;
    private static final double BOX = 70;

    private final double[] farX = new double[FAR], farY = new double[FAR], farZ = new double[FAR];
    private final double[] farSize = new double[FAR];
    private final Color[] farColor = new Color[FAR];
    private final double[] nearX = new double[NEAR], nearY = new double[NEAR], nearZ = new double[NEAR];
    private final double[] nearSize = new double[NEAR];
    private final Color[] nearColor = new Color[NEAR];
    private final double[] scratch = new double[3];
    private final double[] scratchTail = new double[3];
    private Image nebula;
    private Color base = Color.web("#03081A");
    private double sunX = -.52, sunY = -.42, sunZ = -.74;

    SpaceBackdrop(String palette) {
        var random = new SplittableRandom(77);
        for (int i = 0; i < FAR; i++) {
            double u = random.nextDouble(-1, 1);
            double angle = random.nextDouble(0, Math.TAU);
            double r = Math.sqrt(1 - u * u);
            farX[i] = r * Math.cos(angle);
            farY[i] = u;
            farZ[i] = r * Math.sin(angle);
            farSize[i] = Math.pow(random.nextDouble(), 3) * 2.4 + .7;
            farColor[i] = starColor(random);
        }
        for (int i = 0; i < NEAR; i++) {
            nearX[i] = random.nextDouble(-BOX, BOX);
            nearY[i] = random.nextDouble(-BOX, BOX);
            nearZ[i] = random.nextDouble(-BOX, BOX);
            nearSize[i] = random.nextDouble(.8, 2.6);
            nearColor[i] = starColor(random);
        }
        nebula = NEBULAS.computeIfAbsent(palette, SpaceBackdrop::paintNebula);
        setMouseTransparent(true);
    }

    void setBase(Color color) {
        base = color;
    }

    void setSun(double x, double y, double z) {
        double length = Math.sqrt(x * x + y * y + z * z);
        sunX = x / length;
        sunY = y / length;
        sunZ = z / length;
    }

    /**
     * @param flow       how fast space streams past, in world units per second along -Z
     * @param dt         seconds since the previous frame
     * @param streakTime how long a star's trail is, in seconds of travel
     * @param skyFade    0 shows space; 1 hides it behind a daytime sky (used right at launch)
     */
    void draw(CameraRig rig, double flow, double dt, double streakTime, double time, Color skyColor, double skyFade) {
        var g = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();
        if (w < 2 || h < 2) {
            return;
        }
        g.setGlobalBlendMode(BlendMode.SRC_OVER);
        g.setGlobalAlpha(1);
        g.setFill(base);
        g.fillRect(0, 0, w, h);
        double focal = (h / 2) / Math.tan(Math.toRadians(rig.fieldOfView() / 2));
        drawNebula(g, rig, w, h);
        drawSun(g, rig, w, h, focal);

        g.setGlobalBlendMode(BlendMode.ADD);
        for (int i = 0; i < FAR; i++) {
            rig.toCamera(farX[i], farY[i], farZ[i], scratch);
            if (scratch[2] <= .05) {
                continue;
            }
            double sx = w / 2 + focal * scratch[0] / scratch[2];
            double sy = h / 2 + focal * scratch[1] / scratch[2];
            if (sx < -4 || sy < -4 || sx > w + 4 || sy > h + 4) {
                continue;
            }
            double twinkle = .75 + .25 * Math.sin(time * (1.3 + i % 7) + i);
            g.setGlobalAlpha(Math.clamp(farColor[i].getOpacity() * twinkle, 0, 1));
            g.setFill(farColor[i]);
            double size = farSize[i];
            g.fillOval(sx - size / 2, sy - size / 2, size, size);
            if (size > 2.4) {
                g.setGlobalAlpha(.35 * twinkle);
                g.fillRect(sx - size * 1.6, sy - .4, size * 3.2, .8);
                g.fillRect(sx - .4, sy - size * 1.6, .8, size * 3.2);
            }
        }

        double move = flow * dt;
        for (int i = 0; i < NEAR; i++) {
            nearZ[i] -= move;
            double rx = wrap(nearX[i] - rig.x);
            double ry = wrap(nearY[i] - rig.y);
            double rz = wrap(nearZ[i] - rig.z);
            rig.toCamera(rx, ry, rz, scratch);
            if (scratch[2] <= .6) {
                continue;
            }
            double sx = w / 2 + focal * scratch[0] / scratch[2];
            double sy = h / 2 + focal * scratch[1] / scratch[2];
            double distance = Math.sqrt(rx * rx + ry * ry + rz * rz);
            double fade = Math.clamp(1 - distance / BOX, 0, 1);
            if (fade <= 0 || sx < -50 || sy < -50 || sx > w + 50 || sy > h + 50) {
                continue;
            }
            g.setGlobalAlpha(fade * .95);
            double tail = Math.min(flow * streakTime, 40);
            if (tail > .4) {
                rig.toCamera(rx, ry, rz + tail, scratchTail);
                double tz = Math.max(.6, scratchTail[2]);
                double tx = w / 2 + focal * scratchTail[0] / tz;
                double ty = h / 2 + focal * scratchTail[1] / tz;
                g.setStroke(nearColor[i]);
                g.setLineWidth(nearSize[i] * (.5 + fade));
                g.strokeLine(tx, ty, sx, sy);
            } else {
                double size = nearSize[i] * (.6 + fade * 1.2);
                g.setFill(nearColor[i]);
                g.fillOval(sx - size / 2, sy - size / 2, size, size);
            }
        }
        g.setGlobalBlendMode(BlendMode.SRC_OVER);
        if (skyFade > 0) {
            g.setGlobalAlpha(Math.clamp(skyFade, 0, 1));
            g.setFill(skyColor);
            g.fillRect(0, 0, w, h);
        }
        g.setGlobalAlpha(1);
    }

    private void drawNebula(GraphicsContext g, CameraRig rig, double w, double h) {
        double perDegree = h / rig.fieldOfView();
        double panoW = 360 * perDegree;
        double panoH = 180 * perDegree;
        double yaw = ((Math.toDegrees(rig.yaw) % 360) + 360) % 360;
        double pitch = Math.toDegrees(rig.pitch);
        double left = w / 2 - yaw / 360 * panoW;
        double top = h / 2 - (90 - pitch) * perDegree;
        g.save();
        g.translate(w / 2, h / 2);
        g.rotate(-Math.toDegrees(rig.roll));
        g.translate(-w / 2, -h / 2);
        for (int copy = -1; copy <= 1; copy++) {
            double x = left + copy * panoW;
            if (x + panoW < -w || x > 2 * w) {
                continue;
            }
            g.drawImage(nebula, x, top, panoW, panoH);
        }
        g.restore();
    }

    private void drawSun(GraphicsContext g, CameraRig rig, double w, double h, double focal) {
        rig.toCamera(sunX, sunY, sunZ, scratch);
        if (scratch[2] <= .05) {
            return;
        }
        double sx = w / 2 + focal * scratch[0] / scratch[2];
        double sy = h / 2 + focal * scratch[1] / scratch[2];
        double radius = h * .55;
        if (sx < -radius || sy < -radius || sx > w + radius || sy > h + radius) {
            return;
        }
        g.setFill(new RadialGradient(0, 0, sx, sy, radius, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#FFFDF0")), new Stop(.05, Color.web("#FFF4C8")),
                new Stop(.14, Color.web("#FFD36E", .55)), new Stop(.4, Color.web("#FF9A4A", .14)),
                new Stop(1, Color.TRANSPARENT)));
        g.fillOval(sx - radius, sy - radius, radius * 2, radius * 2);
    }

    private static double wrap(double value) {
        double size = BOX * 2;
        value = (value + BOX) % size;
        if (value < 0) {
            value += size;
        }
        return value - BOX;
    }

    private static Color starColor(SplittableRandom random) {
        double pick = random.nextDouble();
        double alpha = random.nextDouble(.45, 1);
        if (pick < .62) {
            return Color.color(.92, .96, 1, alpha);
        }
        if (pick < .82) {
            return Color.color(.62, .82, 1, alpha);
        }
        if (pick < .94) {
            return Color.color(1, .9, .64, alpha);
        }
        return Color.color(1, .66, .82, alpha);
    }

    /** Paints an equirectangular nebula with seamless 3D noise. */
    private static Image paintNebula(String palette) {
        Color[] colors = switch (palette) {
            case "mars" -> new Color[]{Color.web("#6A1E5C"), Color.web("#E0533C"), Color.web("#FFB061"), Color.web("#2A2C8F")};
            case "moon" -> new Color[]{Color.web("#2C2A8F"), Color.web("#9D3FD6"), Color.web("#3FD8E8"), Color.web("#E85DB4")};
            default -> new Color[]{Color.web("#123E8F"), Color.web("#2ED1C6"), Color.web("#8D4FE0"), Color.web("#4DA3FF")};
        };
        int width = 720;
        int height = 360;
        var image = new WritableImage(width, height);
        PixelWriter writer = image.getPixelWriter();
        Color dark = Color.web("#03081A");
        for (int j = 0; j < height; j++) {
            double lat = Math.PI / 2 - Math.PI * (j + .5) / height;
            double cy = -Math.sin(lat);
            double cl = Math.cos(lat);
            for (int i = 0; i < width; i++) {
                double lon = Math.TAU * (i + .5) / width;
                double x = cl * Math.sin(lon);
                double z = cl * Math.cos(lon);
                double cloud = fbm(x * 2.1 + 11, cy * 2.1 + 3, z * 2.1 - 7, 5);
                double tint = fbm(x * 1.3 - 5, cy * 1.3 + 9, z * 1.3 + 2, 3);
                double wisps = fbm(x * 5.5, cy * 5.5, z * 5.5, 3);
                double band = Math.exp(-Math.pow((cy + .25 * Math.sin(lon * 2)) / .55, 2));
                double density = Math.clamp((cloud - .42) * 2.6 * (.45 + .75 * band) + (wisps - .5) * .35, 0, 1);
                Color a = colors[0].interpolate(colors[1], Math.clamp(tint * 1.6 - .3, 0, 1));
                Color b = colors[2].interpolate(colors[3], Math.clamp(wisps * 1.8 - .4, 0, 1));
                Color mix = a.interpolate(b, Math.clamp((cloud - .5) * 3, 0, 1));
                Color pixel = dark.interpolate(mix, density * .78);
                double glow = Math.pow(density, 3) * .35;
                pixel = pixel.interpolate(Color.WHITE, glow * .5);
                writer.setColor(i, j, pixel);
            }
        }
        return image;
    }

    private static double fbm(double x, double y, double z, int octaves) {
        double total = 0;
        double amplitude = .5;
        double norm = 0;
        for (int i = 0; i < octaves; i++) {
            total += amplitude * noise(x, y, z);
            norm += amplitude;
            x *= 2.03;
            y *= 2.03;
            z *= 2.03;
            amplitude *= .5;
        }
        return total / norm;
    }

    private static double noise(double x, double y, double z) {
        int xi = (int) Math.floor(x), yi = (int) Math.floor(y), zi = (int) Math.floor(z);
        double xf = x - xi, yf = y - yi, zf = z - zi;
        double u = xf * xf * (3 - 2 * xf), v = yf * yf * (3 - 2 * yf), w = zf * zf * (3 - 2 * zf);
        double c000 = hash(xi, yi, zi), c100 = hash(xi + 1, yi, zi);
        double c010 = hash(xi, yi + 1, zi), c110 = hash(xi + 1, yi + 1, zi);
        double c001 = hash(xi, yi, zi + 1), c101 = hash(xi + 1, yi, zi + 1);
        double c011 = hash(xi, yi + 1, zi + 1), c111 = hash(xi + 1, yi + 1, zi + 1);
        double x00 = c000 + (c100 - c000) * u, x10 = c010 + (c110 - c010) * u;
        double x01 = c001 + (c101 - c001) * u, x11 = c011 + (c111 - c011) * u;
        double y0 = x00 + (x10 - x00) * v, y1 = x01 + (x11 - x01) * v;
        return y0 + (y1 - y0) * w;
    }

    private static double hash(int x, int y, int z) {
        long h = x * 374761393L + y * 668265263L + z * 2147483647L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (double) 0xFFFFFF;
    }
}
