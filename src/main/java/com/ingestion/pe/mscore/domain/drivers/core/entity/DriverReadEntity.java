package com.ingestion.pe.mscore.domain.drivers.core.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "drivers", schema = "drivers_module")
public class DriverReadEntity {

    @Id
    @Column(nullable = false, insertable = false, updatable = false)
    private Long id;

    @Column(name = "document_number", insertable = false, updatable = false)
    private String documentNumber;

    @Column(name = "company_id", insertable = false, updatable = false)
    private Long companyId;

    @Column(insertable = false, updatable = false)
    private String name;

    @Column(name = "last_name", insertable = false, updatable = false)
    private String lastName;
}
