package com.example.javarelay.infra;

public class GeoUtil {

    private static final double EARTH_RADIUS = 6378137.0;


    public static double getDistance(double lng1, double lat1, double lng2, double lat2) {

        double radLat1 = Math.toRadians(lat1);
        double radLat2 = Math.toRadians(lat2);
        double radLng1 = Math.toRadians(lng1);
        double radLng2 = Math.toRadians(lng2);

        double deltaLat = radLat1 - radLat2;
        double deltaLng = radLng1 - radLng2;


        double a = Math.pow(Math.sin(deltaLat / 2), 2) +
                Math.cos(radLat1) * Math.cos(radLat2) *
                        Math.pow(Math.sin(deltaLng / 2), 2);

        double c = 2 * Math.asin(Math.sqrt(a));


        return c * EARTH_RADIUS;
    }

}