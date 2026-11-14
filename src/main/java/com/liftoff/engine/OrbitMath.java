package com.liftoff.engine;

import com.liftoff.model.Orbit;


public final class OrbitMath {


    private static final double EARTH_RADIUS_KM = 6378.137;


    private static final double EARTH_MU = 398_600.4418;

    private static final double MINUTES_PER_DAY = 1440;


    private static final int ORBIT_SAMPLES = 7200;

    private OrbitMath() {
    }


    public static double periodMinutes(double altitudeKm) {
        double radiusKm = EARTH_RADIUS_KM + altitudeKm;
        double seconds = 2 * Math.PI * Math.sqrt(radiusKm * radiusKm * radiusKm / EARTH_MU);
        return seconds / 60;
    }


    public static double maxLatitudeDeg(double inclinationDeg) {
        return inclinationDeg <= 90 ? inclinationDeg : 180 - inclinationDeg;
    }


    public static double viewAngleDeg(double altitudeKm, double minElevationDeg) {
        double elevation = Math.toRadians(minElevationDeg);
        double angle = Math.acos(EARTH_RADIUS_KM * Math.cos(elevation) / (EARTH_RADIUS_KM + altitudeKm)) - elevation;
        return Math.toDegrees(angle);
    }


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


    private static double longitudeShareInView(double latitude, double stationLatitude, double viewAngle) {
        double sinProduct = Math.sin(stationLatitude) * Math.sin(latitude);
        double cosProduct = Math.cos(stationLatitude) * Math.cos(latitude);
        if (cosProduct < 1e-12) {
            // Longitude is undefined at a pole.
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
