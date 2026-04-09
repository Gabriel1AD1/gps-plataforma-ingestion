package com.ingestion.pe.mscore.domain.devices.core.entity;

import com.ingestion.pe.mscore.commons.libs.EngineJexl;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "override_sensors", schema = "devices_module")
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class OverrideSensorsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;

    @Column(name = "jexl_script", nullable = false, columnDefinition = "text")
    private String jexlScript;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(name = "max_devices_used", nullable = false)
    @Comment("Número máximo de dispositivos que pueden usar esta configuración")
    private Long maxDevicesUsed;

    @CreationTimestamp
    private Instant created;
    @UpdateTimestamp
    private Instant updated;

    @Column(name = "company_id", nullable = false)
    @Comment("Empresa a la que pertenece")
    private Long companyId;

    public Map<String, Object> executeJexlScript(Map<String, Object> attributes) {
        return EngineJexl.overrideAttributes(attributes, this.jexlScript);
    }

}
