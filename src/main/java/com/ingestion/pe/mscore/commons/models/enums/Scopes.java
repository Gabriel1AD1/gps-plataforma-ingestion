package com.ingestion.pe.mscore.commons.models.enums;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public enum Scopes {
  tenant_add_access("tenant.add.access"),
  tenant_remove_access("tenant.remove.access"),
  tenant_create("tenant.create"),
  tenant_update("tenant.update"),
  tenant_read("tenant.read"),
  tenant_delete("tenant.delete"),
  user_add_exclusion_to_device("user.exclusion.add.device"),
  user_remove_exclusion_to_device("user.exclusion.remove.device"),
  user_create("user.create"),
  user_delete("user.delete"),
  user_update("user.update"),
  user_read("user.read"),
  user_add_role("user.add.role"),
  user_remove_role("user.remove.role"),
  user_add_tag("user.add.tag"),
  user_remove_tag("user.remove.tag"),
  user_add_group("user.add.group"),
  user_remove_group("user.remove.group"),
  user_add_attachment("user.add.attachment"),
  user_remove_attachment("user.remove.attachment"),
  user_remove_exclusion_application("user.exclusion.remove.application"),
  user_add_exclusion_application("user.exclusion.add.application"),
  role_add_to_user("role.add.user"),
  role_remove_to_user("role.remove.user"),
  device_create("device.create"),
  device_delete("device.delete"),
  device_update("device.update"),
  device_read("device.read"),
  device_add_override_sensors("device.add.override.sensor"),
  device_add_command("device.command.add"),
  device_remove_command("device.command.remove"),
  device_add_attachment("device.add.attachment"),
  device_remove_attachment("device.remove.attachment"),
  device_add_tag("device.add.tag"),
  device_remove_tag("device.remove.tag"),
  device_add_group("device.add.group"),
  device_remove_group("device.remove.group"),

  override_sensors_create("override.sensor.create"),
  override_sensors_read("override.sensor.read"),
  override_sensors_update("override.sensor.update"),
  override_sensors_delete("override.sensor.delete"),

  command_create("command.create"),
  command_delete("command.delete"),
  command_update("command.update"),
  command_read("command.read"),

  command_add_device("command.add.device"),
  command_remove_device("command.remove.device"),

  config_alert_create("config.alert.create"),
  config_alert_delete("config.alert.delete"),
  config_alert_update("config.alert.update"),
  config_alert_read("config.alert.read"),
  config_alert_add_device("config.alert.add.device"),
  config_alert_remove_device("config.alert.remove.device"),

  attachment_create("attachment.create"),
  attachment_delete("attachment.delete"),
  attachment_update("attachment.update"),
  attachment_read("attachment.read"),

  group_create("group.create"),
  group_delete("group.delete"),
  group_update("group.update"),
  group_read("group.read"),

  tag_create("tag.create"),
  tag_delete("tag.delete"),
  tag_update("tag.update"),
  tag_read("tag.read"),
  driver_create("driver.create"),
  driver_delete("driver.delete"),
  driver_update("driver.update"),
  driver_read("driver.read"),

  geofence_create("geofence.create"),
  geofence_update("geofence.update"),
  geofence_delete("geofence.delete"),
  geofence_read("geofence.read"),

  geofence_add_group("geofence.add.group"),
  geofence_remove_group("geofence.remove.group"),

  geofence_add_tag("geofence.add.tag"),
  geofence_remove_tag("geofence.remove.tag"),

  driver_add_phone("driver.add.phone"),
  driver_remove_phone("driver.remove.phone"),

  driver_add_email("driver.add.email"),
  driver_remove_email("driver.remove.email"),

  driver_add_document("driver.add.document"),
  driver_remove_document("driver.remove.document"),

  driver_add_tag("driver.add.tag"),
  driver_remove_tag("driver.remove.tag"),

  driver_add_group("driver.add.group"),
  driver_remove_group("driver.remove.group"),

  driver_add_attachment("driver.add.attachment"),
  driver_remove_attachment("driver.remove.attachment"),


  vehicle_create("vehicle.create"),
  vehicle_delete("vehicle.delete"),
  vehicle_update("vehicle.update"),
  vehicle_read("vehicle.read"),
  vehicle_add_tag("vehicle.add.tag"),
  vehicle_remove_tag("vehicle.remove.tag"),
  vehicle_add_group("vehicle.add.group"),
  vehicle_remove_group("vehicle.remove.group"),
  vehicle_add_attachment("vehicle.add.attachment"),
  vehicle_remove_attachment("vehicle.remove.attachment"),
  vehicle_add_geofence("vehicle.add.geofence"),
  vehicle_remove_geofence("vehicle.remove.geofence"),
  vehicle_update_geofence_notification("vehicle.update.geofence.notification"),
  vehicle_add_driver("vehicle.add.driver"),
  vehicle_remove_driver("vehicle.remove.driver"),
  undefined("undefined");

  private final String value;

  Scopes(String value) {
    this.value = value;
  }

  public static Scopes fromValue(String value) {
    for (Scopes b : Scopes.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    log.error("No Scopes enum constant with value: {}", value);
    return Scopes.undefined;
  }


  /**
   * @param dbData cadena de texto que viene de la base de datos
   * @return Convierte una cadena de texto a un valor del enum Scopes.
   */
  public static Scopes fromString(String dbData) {
    try {
      return Scopes.valueOf(dbData.toLowerCase());
    } catch (Exception e) {
      log.error("Error converting Scopes from string: {}", dbData, e);
      return Scopes.undefined;
    }
  }
  /**
   * @return Retorna el valor del enum como cadena.
   */
  public String value() {
    return value;
  }

}
