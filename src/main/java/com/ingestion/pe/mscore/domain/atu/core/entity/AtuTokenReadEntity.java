package com.ingestion.pe.mscore.domain.atu.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "atu_token", schema = "devices_module")
public class AtuTokenReadEntity {

    @Id
    @Column(nullable = false, insertable = false, updatable = false)
    private Long id;

    @Column(name = "company_id", nullable = false, insertable = false, updatable = false)
    private Long companyId;

    @Column(nullable = false, insertable = false, updatable = false)
    private String token;

    @Column(nullable = false, insertable = false, updatable = false)
    private String endpoint;

    @Column(nullable = false, insertable = false, updatable = false)
    private boolean active;

    @Column(insertable = false, updatable = false)
    private Instant created;

    @Column(insertable = false, updatable = false)
    private Instant updated;
}
