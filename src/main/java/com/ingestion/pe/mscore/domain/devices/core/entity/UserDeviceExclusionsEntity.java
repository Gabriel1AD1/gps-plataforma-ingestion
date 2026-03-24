package com.ingestion.pe.mscore.domain.devices.core.entity;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "user_device_exclusions", schema = "devices_module")
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class UserDeviceExclusionsEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(nullable = false)
  private Long id;

  @Column(name = "user_uuuid", nullable = false)
  @Comment("El id del usuario que esta excluido de ver el dispositivo")
  private UUID userUuid;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "device_id", nullable = false)
  @Comment("El id del dispositivo asociado a un usuario excluido")
  @OnDelete(action = OnDeleteAction.CASCADE)
  private DeviceEntity device;
}
