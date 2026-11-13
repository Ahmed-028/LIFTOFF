package com.liftoff.engine;

import com.liftoff.model.Orbit;

/** First-order orbit calculations for circular Earth orbits. */
public final class OrbitMath {

    /** Earth's equatorial radius, km (WGS 84). */
    private static final double EARTH_RADIUS_KM = 6378.137;

    /** Earth's gravitational parameter, km^3/s^2. */
    private static final double EARTH_MU = 398_600.4418;

    private static final double MINUTES_PER_DAY = 1440;

    /** Steps around the orbit used to average the time in view. */
    private static final int ORBIT_SAMPLES = 7200;

    private OrbitMath() {
    }

    /** Time for one lap of a circular orbit at the given altitude, in minutes: T = 2 pi sqrt(a^3 / mu). */
    public static double periodMinutes(double altitudeKm) {
        double radiusKm = EARTH_RADIUS_KM + altitudeKm;
        double seconds = 2 * Math.PI * Math.sqrt(radiusKm * radiusKm * radiusKm / EARTH_MU);
        return seconds / 60;
    }

    /** Highest latitude, north or south, that an orbit of this inclination flies over, in degrees. */
    public static double maxLatitudeDeg(double inclinationDeg) {
        return inclinationDeg <= 90 ? inclinationDeg : 180 - inclinationDeg;
    }

    /**
     * Angle at Earth's centre between a ground station and the furthest point above which it can
     * still see the spacecraft, in degrees: arccos(R cos e / (R + h)) - e.
     */
    public static double viewAngleDeg(double altitudeKm, double minElevationDeg) {
        double elevation = Math.toRadians(minElevationDeg);
        double angle = Math.acos(EARTH_RADIUS_KM * Math.cos(elevation) / (EARTH_RADIUS_KM + altitudeKm)) - elevation;
        return Math.toDegrees(angle);
    }

    /**
     * Long-run average minutes per day the spacecraft is in view of a station at the given latitude.
     *
     * <p>Over many days the point under the spacecraft visits every longitude equally, so the share
     * of time in view is the average, around the orbit, of the share of longitudes that lie within
     * the station's view angle at the spacecraft's current latitude.
     */
    public static double contactMinutesPerDay(Orbit orbit, double stationLatitudeDeg) {
        double viewAngle = Math.toRadians(viewAngleDeg(orbit.altitudeKm(), GameRules.MIN_ELEVATION_DEG));
        double inclination = Math.toRadians(orbit.inclinationDeg());
        double stationLatitude = Math.toRadians(stationLatitudeDeg);

        double shareInView = 0;
        for (int step = 0; step < ORBIT_SAMPLES; step++) {
            double positionAlongOrbit = 2 * Math.PI * (step + 0.5) / ORBIT_SAMPLES;
            double latitude = Math.asin(Math.sin(inclination) * Math.sin(positionAlongOrbit));
            shareInView += longitudeShareInView(latitude, stationLatitude, viewAngle);
        }
        return MINUTES_PER_DAY * shareInView / ORBIT_SAMPLES;
    }

    /**
     * Share (0 to 1) of all longitudes at which a point at the given latitude is within the view
     * angle of the station. From the spherical law of cosines for the angle between the two points.
     */
    private static double longitudeShareInView(double latitude, double stationLatitude, double viewAngle) {
        double sinProduct = Math.sin(stationLatitude) * Math.sin(latitude);
        double cosProduct = Math.cos(stationLatitude) * Math.cos(latitude);
        if (cosProduct < 1e-12) {
            // Station or spacecraft is over a pole: longitude no longer matters.
            return sinProduct >= Math.cos(viewAngle) ? 1 : 0;
        }
        double cosHalfWidth = (Math.cos(viewAngle) - sinProduct) / cosProduct;
        if (cosHalfWidth >= 1) {
            return 0;
        }
        if (cosHalfWidth <= -1) {
            return 1;
        }
        return Math.acos(cosHalfWidth) / Math.PI;
    }
}
