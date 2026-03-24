package com.ingestion.pe.mscore.applications.tracking;

import static org.junit.jupiter.api.Assertions.*;

import com.ingestion.pe.mscore.commons.models.GeofenceResponse;
import com.ingestion.pe.mscore.commons.models.PointsResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GeofenceEvaluator-Unit Tests")
class GeofenceEvaluatorTest {

    private GeofenceEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new GeofenceEvaluator();
    }

    @Test
    @DisplayName("Retorna falso cuando la geocerca es nula o no tiene tipo")
    void isInside_NullGeofenceOrType() {
        assertFalse(evaluator.isInside(null, 10.0, 10.0));
        assertFalse(evaluator.isInside(new GeofenceResponse(), 10.0, 10.0));
    }

    @Test
    @DisplayName("CÍRCULO - Coordenadas dentro")
    void isInside_Circle_Inside() {
        GeofenceResponse circle = new GeofenceResponse();
        circle.setType("CIRCLE");
        circle.setLatitudeCenter(10.000000);
        circle.setLongitudeCenter(10.000000);
        circle.setRadiusInMeters(1000.0);

        assertTrue(evaluator.isInside(circle, 10.001, 10.001));
    }

    @Test
    @DisplayName("CÍRCULO - Coordenadas fuera")
    void isInside_Circle_Outside() {
        GeofenceResponse circle = new GeofenceResponse();
        circle.setType("CIRCLE");
        circle.setLatitudeCenter(10.000000);
        circle.setLongitudeCenter(10.000000);
        circle.setRadiusInMeters(1000.0);

        assertFalse(evaluator.isInside(circle, 10.05, 10.05));
    }

    @Test
    @DisplayName("CÍRCULO - Parámetros faltantes retorna falso")
    void isInside_Circle_MissingParams() {
        GeofenceResponse circle = new GeofenceResponse();
        circle.setType("CIRCLE");
        circle.setRadiusInMeters(1000.0);

        assertFalse(evaluator.isInside(circle, 10.0, 10.0));
    }

    @Test
    @DisplayName("POLÍGONO - Coordenadas dentro")
    void isInside_Polygon_Inside() {
        GeofenceResponse polygon = new GeofenceResponse();
        polygon.setType("POLYGON");

        PointsResponse p1 = new PointsResponse();
        p1.setLatitude(0.0);
        p1.setLongitude(0.0);
        PointsResponse p2 = new PointsResponse();
        p2.setLatitude(10.0);
        p2.setLongitude(0.0);
        PointsResponse p3 = new PointsResponse();
        p3.setLatitude(10.0);
        p3.setLongitude(10.0);
        PointsResponse p4 = new PointsResponse();
        p4.setLatitude(0.0);
        p4.setLongitude(10.0);

        polygon.setPoints(List.of(p1, p2, p3, p4));

        assertTrue(evaluator.isInside(polygon, 5.0, 5.0));
    }

    @Test
    @DisplayName("POLÍGONO - Coordenadas fuera")
    void isInside_Polygon_Outside() {
        GeofenceResponse polygon = new GeofenceResponse();
        polygon.setType("POLYGON");

        PointsResponse p1 = new PointsResponse();
        p1.setLatitude(0.0);
        p1.setLongitude(0.0);
        PointsResponse p2 = new PointsResponse();
        p2.setLatitude(10.0);
        p2.setLongitude(0.0);
        PointsResponse p3 = new PointsResponse();
        p3.setLatitude(10.0);
        p3.setLongitude(10.0);
        PointsResponse p4 = new PointsResponse();
        p4.setLatitude(0.0);
        p4.setLongitude(10.0);

        polygon.setPoints(List.of(p1, p2, p3, p4));

        assertFalse(evaluator.isInside(polygon, 5.0, 15.0));
    }

    @Test
    @DisplayName("POLÍGONO - Tamaño inválido retorna falso")
    void isInside_Polygon_InvalidSize() {
        GeofenceResponse polygon = new GeofenceResponse();
        polygon.setType("POLYGON");

        PointsResponse p1 = new PointsResponse();
        p1.setLatitude(0.0);
        p1.setLongitude(0.0);
        PointsResponse p2 = new PointsResponse();
        p2.setLatitude(10.0);
        p2.setLongitude(0.0);

        polygon.setPoints(List.of(p1, p2));

        assertFalse(evaluator.isInside(polygon, 5.0, 5.0));
    }

    @Test
    @DisplayName("Lanza excepción cuando el tipo es inesperado")
    void isInside_UnexpectedType_ThrowsException() {
        GeofenceResponse unknown = new GeofenceResponse();
        unknown.setType("LINE");

        Exception ex = assertThrows(IllegalStateException.class, () -> evaluator.isInside(unknown, 10.0, 10.0));
        assertTrue(ex.getMessage().contains("tipo inesperado"));
    }
}