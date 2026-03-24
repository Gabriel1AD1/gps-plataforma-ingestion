package com.ingestion.pe.mscore.domain.geofences.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "geofences", schema = "geofences_module")
public class GeofenceReadEntity {

    @Id
    @Column(nullable = false, insertable = false, updatable = false)
    private Long id;

    @Column(name = "company_id", nullable = false, insertable = false, updatable = false)
    private Long companyId;

    @Column(nullable = false, insertable = false, updatable = false)
    private String name;

    @Column(length = 500, insertable = false, updatable = false)
    private String description;

    @Column(nullable = false, name = "latitude_center", insertable = false, updatable = false)
    private Double latitudeCenter;

    @Column(nullable = false, name = "longitude_center", insertable = false, updatable = false)
    private Double longitudeCenter;

    @Column(nullable = false, insertable = false, updatable = false)
    private Double radiusInMeters;

    @Column(nullable = false, insertable = false, updatable = false)
    private String type;

    @Column(length = 5000, insertable = false, updatable = false)
    private String points;
}
