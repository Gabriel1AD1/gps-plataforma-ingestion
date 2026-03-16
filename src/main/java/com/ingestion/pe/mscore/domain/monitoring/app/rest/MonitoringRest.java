package com.ingestion.pe.mscore.domain.monitoring.app.rest;

import com.ingestion.pe.mscore.domain.monitoring.app.service.MonitoringService;
import com.ingestion.pe.mscore.domain.monitoring.core.model.DateroResult;
import com.ingestion.pe.mscore.domain.monitoring.core.model.LinearViewResult;
import com.ingestion.pe.mscore.domain.monitoring.core.model.TripState;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mnt/api/v1/monitoring")
@RequiredArgsConstructor
public class MonitoringRest {

    private final MonitoringService monitoringService;

    @GetMapping("/linear-view")
    public ResponseEntity<List<LinearViewResult>> getLinearView(@RequestParam Long routeId) {
        return ResponseEntity.ok(monitoringService.getLinearView(routeId));
    }

    @GetMapping("/datero")
    public ResponseEntity<List<DateroResult>> getDatero(@RequestParam Long routeId) {
        return ResponseEntity.ok(monitoringService.getDatero(routeId));
    }

    @GetMapping("/trip/{tripId}/state")
    public ResponseEntity<TripState> getTripState(@PathVariable Long tripId) {
        return monitoringService.getTripState(tripId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "activeTrips", monitoringService.getActiveTripsCount()));
    }
}
