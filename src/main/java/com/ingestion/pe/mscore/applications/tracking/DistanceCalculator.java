package com.ingestion.pe.mscore.applications.tracking;

import com.ingestion.pe.mscore.commons.models.Position;
import com.ingestion.pe.mscore.commons.validators.CoordinateValidator;
import com.ingestion.pe.mscore.config.cache.CacheDao;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class DistanceCalculator {

    private static final long CACHE_TTL_SECONDS = 86400; // 24 hours
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final CacheDao<DistanceData> cacheDao;
    private final CoordinateValidator coordinateValidator;

    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        if (!coordinateValidator.isValidCoordinate(lat1, lon1) ||
                !coordinateValidator.isValidCoordinate(lat2, lon2)) {
            log.warn("Coordenadas invalidas: [{}, {}] a [{}, {}]", lat1, lon1, lat2, lon2);
            return 0.0;
        }

        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS_KM * c;

        log.trace("Distancia calculada: {:.2f} km", distance);
        return distance;
    }

    public double getDistanceToday(String imei, Long companyId) {
        if (imei == null || companyId == null) {
            return 0.0;
        }

        String key = buildDistanceKey(companyId, imei);
        return cacheDao.get(key, DistanceData.class)
                .map(DistanceData::getCumulativeDistance)
                .orElse(0.0);
    }

    public void updateDistance(Position newPosition, Long companyId) {
        if (newPosition == null || companyId == null || newPosition.getImei() == null) {
            log.warn("No se puede actualizar distancia: parametros invalidos (position={}, companyId={}, imei={})",
                    newPosition != null, companyId, newPosition != null ? newPosition.getImei() : null);
            return;
        }

        if (!coordinateValidator.isValidCoordinate(newPosition.getLatitude(), newPosition.getLongitude())) {
            log.warn("Coordenadas invalidas para el IMEI {}: lat={}, lon={}",
                    newPosition.getImei(), newPosition.getLatitude(), newPosition.getLongitude());
            return;
        }

        String cacheKey = buildDistanceKey(companyId, newPosition.getImei());

        try {
            Optional<DistanceData> existingDataOpt = cacheDao.get(cacheKey, DistanceData.class);

            if (existingDataOpt.isEmpty()) {
                cacheDao.save(cacheKey, new DistanceData(newPosition, 0.0), CACHE_TTL_SECONDS);
                log.debug("Seguimiento de distancia inicializado para IMEI: {}", newPosition.getImei());
            } else {
                DistanceData existingData = existingDataOpt.get();
                double delta = calculateDistance(
                        existingData.getLastPosition().getLatitude(),
                        existingData.getLastPosition().getLongitude(),
                        newPosition.getLatitude(),
                        newPosition.getLongitude());

                double newTotal = existingData.getCumulativeDistance() + delta;
                cacheDao.save(cacheKey, new DistanceData(newPosition, newTotal), CACHE_TTL_SECONDS);
                log.debug("Distancia actualizada para IMEI {}: +{:.2f} km, total: {:.2f} km",
                        newPosition.getImei(), delta, newTotal);
            }
        } catch (Exception e) {
            log.error("Fallo al actualizar distancia para el IMEI {} en Redis caché: {}",
                    newPosition.getImei(), e.getMessage(), e);
        }
    }

    private String buildDistanceKey(Long companyId, String imei) {
        return "distance:" + companyId + ":" + imei;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DistanceData {
        private Position lastPosition;
        private double cumulativeDistance;
    }
}
