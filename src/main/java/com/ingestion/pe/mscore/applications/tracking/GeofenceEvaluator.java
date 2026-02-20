package com.ingestion.pe.mscore.applications.tracking;

import com.ingestion.pe.mscore.commons.models.GeofenceResponse;
import com.ingestion.pe.mscore.commons.models.PointsResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GeofenceEvaluator {

    private static final double EARTH_RADIUS_METERS = 6371000;

    public boolean isInside(
            GeofenceResponse geofence,
            double latitude,
            double longitude) {

        if (geofence == null || geofence.getType() == null) {
            return false;
        }

        return switch (geofence.getType()) {
            case "CIRCLE" -> isInsideCircle(geofence, latitude, longitude);
            case "POLYGON" -> isInsidePolygon(geofence.getPoints(), latitude, longitude);
            default -> throw new IllegalStateException("Unexpected geofence type: " + geofence.getType());
        };
    }

    private boolean isInsideCircle(
            GeofenceResponse geofence,
            double lat,
            double lng) {

        if (geofence.getLatitudeCenter() == null ||
                geofence.getLongitudeCenter() == null ||
                geofence.getRadiusInMeters() == null) {
            return false;
        }

        double distance = haversineDistance(
                lat,
                lng,
                geofence.getLatitudeCenter(),
                geofence.getLongitudeCenter());

        return distance <= geofence.getRadiusInMeters();
    }

    private boolean isInsidePolygon(
            List<PointsResponse> polygon,
            double lat,
            double lng) {

        if (polygon == null || polygon.size() < 3) {
            return false;
        }

        boolean inside = false;
        int j = polygon.size() - 1;

        for (int i = 0; i < polygon.size(); i++) {
            double xi = polygon.get(i).getLatitude();
            double yi = polygon.get(i).getLongitude();
            double xj = polygon.get(j).getLatitude();
            double yj = polygon.get(j).getLongitude();

            boolean intersect = ((yi > lng) != (yj > lng)) &&
                    (lat < (xj - xi) * (lng - yi) / (yj - yi) + xi);

            if (intersect) {
                inside = !inside;
            }

            j = i;
        }

        return inside;
    }

    private double haversineDistance(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) *
                        Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }
}
