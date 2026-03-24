package com.ingestion.pe.mscore.domain.monitoring.core.calc;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HaversineCalculatorTest {

    @Test
    void distanceKm_samePoint_returnsZero() {
        double dist = HaversineCalculator.distanceKm(-12.046, -77.043, -12.046, -77.043);
        assertEquals(0.0, dist, 0.001);
    }

    @Test
    void distanceKm_knownDistance_LimaCusco() {
        double dist = HaversineCalculator.distanceKm(-12.046, -77.043, -13.532, -71.967);
        assertTrue(dist > 570 && dist < 590, "Distancia Lima-Cusco debe estar entre 570-590 km, fue: " + dist);
    }

    @Test
    void distanceKm_shortDistance_twoBlocks() {
        double dist = HaversineCalculator.distanceKm(-12.0460, -77.0430, -12.0469, -77.0430);
        assertTrue(dist > 0.08 && dist < 0.12, "Distancia corta debe ser ~0.1 km, fue: " + dist);
    }

    @Test
    void isWithinRadius_insideRadius_returnsTrue() {
        assertTrue(HaversineCalculator.isWithinRadius(-12.046, -77.043, -12.046, -77.043, 100));
    }

    @Test
    void isWithinRadius_outsideRadius_returnsFalse() {
        assertFalse(HaversineCalculator.isWithinRadius(-12.046, -77.043, -12.056, -77.043, 100));
    }

    @Test
    void isWithinRadius_exactBorder_returnsTrue() {
        double dist = HaversineCalculator.distanceKm(-12.046, -77.043, -12.0469, -77.043);
        double radiusMeters = dist * 1000;
        assertTrue(HaversineCalculator.isWithinRadius(-12.046, -77.043, -12.0469, -77.043, radiusMeters));
    }
}
