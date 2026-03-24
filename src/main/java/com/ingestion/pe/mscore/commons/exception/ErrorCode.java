package com.ingestion.pe.mscore.commons.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "ErrorCode", description = "Detalle del código de error interno")
public class ErrorCode {
    public static final ErrorCode ENDPOINT_NOT_FOUND = ErrorCode.of("api-01", "endpoint no encontrado");
    public static final ErrorCode VALIDATION_FAILED = ErrorCode.of("valid-001", "Validaciones fallida");
    public static final ErrorCode USER_EMAIL_EXISTS = ErrorCode.of("usr-002", "El email ya existe en el sistema");
    public static final ErrorCode USER_DOCUMENT_EXISTS = ErrorCode.of("usr-003",
                    "Ya existe un usuario con el mismo numero de documento y tipo");
    public static final ErrorCode DEVICE_IMEI_EXIST = ErrorCode.of("dev-001",
                    "El dispositivo con el IMEI ya existe");
    public static final ErrorCode DEVICE_NOT_FOUND = ErrorCode.of("dev-002", "El dispositivo no existe");
    public static final ErrorCode GROUP_DEVICE_NOT_FOUND = ErrorCode.of("dev-003",
                    "El grupo de dispositivos no existe");
    public static final ErrorCode DUPLICATE_GROUP_DEVICE = ErrorCode.of("dev-004",
                    "El dispositivo ya pertenece al grupo de dispositivos");
    public static final ErrorCode ATTACHMENT_NOT_FOUND = ErrorCode.of("att-001", "El archivo no existe");
    public static final ErrorCode TAG_NOT_FOUND = ErrorCode.of("tag-001", "El tag no existe");
    public static final ErrorCode COMMAND_NOT_FOUND = ErrorCode.of("cmd-001", "El comando no existe");
    public static final ErrorCode ERROR_TCP = ErrorCode.of("tcp-001",
                    "Error en la comunicación con el servidor TCP");
    public static final ErrorCode COMPANY_ACCESS_NAME_NOT_FOUND = ErrorCode.of("cmp-001",
                    "El nombre de acceso de la empresa no existe");
    public static final ErrorCode COMPANY_NOT_FOUND = ErrorCode.of("cmp-002", "La empresa no existe");
    public static final ErrorCode COMPANY_TENANT_ACCESS_EXISTS = ErrorCode.of("cmp-003",
                    "Intenta conn otro nombre");
    public static final ErrorCode INTERNAL_SERVER_ERROR = ErrorCode.of("srv-001",
                    "Upps! Ha ocurrido un error interno en el servidor");
    public static final ErrorCode DATABASE_CONFLICT = ErrorCode.of("db-001", "Conflicto en la base de datos");
    public static final ErrorCode BAD_REQUEST = ErrorCode.of("bad-001", "Solicitud incorrecta");
    public static final ErrorCode METHOD_NOT_ALLOWED = ErrorCode.of("meth-001", "Método no permitido");
    public static final ErrorCode USER_CANNOT_ASSIGN_PERMISSIONS_ITSELF = ErrorCode.of("auth-006",
                    "El mismo usuario no puede asignarse un permisos a sí mismo");
    public static final ErrorCode USER_CANNOT_REMOVE_PERMISSIONS = ErrorCode.of("auth-007",
                    "El mismo usuario no puede eliminar sus permisos");
    public static final ErrorCode FORBIDDEN = ErrorCode.of("forb-001",
                    "No tienes permisos para acceder o modificar este recurso");
    public static final ErrorCode DRIVER_NOT_FOUND = ErrorCode.of("drv-001", "El conductor no existe");
    public static final ErrorCode DRIVER_USER_ALREADY_EXISTS = ErrorCode.of("drv-002",
                    "El usuario ya está asociado a otro conductor");
    public static final ErrorCode COMPANY_MAX_CREATED_EXCEEDED = ErrorCode.of("cmp-004",
                    "Ha superado el número máximo de empresas permitidas");
    public static final ErrorCode USER_LIMIT_EXCEEDED = ErrorCode.of("usr-004",
                    "Se ha excedido el límite de usuarios permitidos");
    public static final ErrorCode MAX_DEVICES_CREATED_EXCEEDED = ErrorCode.of("dev-005",
                    "Se ha excedido el límite de dispositivos permitidos");
    public static final ErrorCode MAX_DRIVERS_CREATED_EXCEEDED = ErrorCode.of("drv-003",
                    "Se ha excedido el límite de conductores permitidos");
    public static final ErrorCode USER_NOT_SESSION = ErrorCode.of("auth-002",
                    "El usuario no tiene una sesión activa");
    public static final ErrorCode EXPIRATION_EXPIRED = ErrorCode.of("jwt-001", "EL token a expirado");
    public static final ErrorCode CREDENTIALS_INVALID = ErrorCode.of("auth-004", "Credenciales invalidas");
    public static final ErrorCode USER_NOT_LOGIN_FOR_DELETED = ErrorCode.of("auth-005",
                    "El usuario ha sido eliminado, no puede iniciar sesión");
    public static final ErrorCode NOT_ACCESS_SYSTEM = ErrorCode.of("bad-001",
                    "No eres un usuario valido para el sistema");
    public static final ErrorCode USER_NOT_FOUND = ErrorCode.of("usr-001", "El usuario no existe en el sistema");
    public static final ErrorCode CONFIG_ALERTS_NOT_FOUND = ErrorCode.of("cfg-001",
                    "La configuración de alerta no existe");
    public static final ErrorCode FILE_UPLOAD_ERROR = ErrorCode.of("file-001", "Error al subir el archivo");
    public static final ErrorCode GEOFENCE_MAX_EXCEEDED = ErrorCode.of("geo-001",
                    "Se ha excedido el límite de geocercas permitidas");
    public static final ErrorCode GEOFENCE_NOT_FOUND = ErrorCode.of("geo-002", "La geocerca no existe");
    public static final ErrorCode GEOFENCE_INVALID_POINTS = ErrorCode.of("geo-003",
                    "Los puntos de la geocerca no son válidos para el tipo especificado");
    public static final ErrorCode OVERRIDE_SENSORS_NOT_FOUND = ErrorCode.of("ovs-001",
                    "La configuración de sensores no existe");
    public static final ErrorCode USER_NOT_FOUND_IN_CACHE = ErrorCode.of("auth-003",
                    "El usuario no se encuentra en cache porque el token ha expirado");
    public static final ErrorCode REFRESH_TOKEN_USED = ErrorCode.of("auth-008",
                    "El refresh token ya ha sido utilizado");
    public static final ErrorCode USER_NOT_EQUALS_SESSION_SERVER = ErrorCode.of(
                    "auth-009",
                    "El usuario del token no coincide con el usuario de la sesión en el servidor");
    public static final ErrorCode USER_NOT_FOUND_IN_SESSION_SERVER = ErrorCode.of("auth-010",
                    "El usuario no se encuentra en la sesión del servidor");
    public static final ErrorCode COMPANY_NOT_PARENT = ErrorCode.of(
                    "cmp-005", "La empresa ala cual intenta cambiar no es una empresa creada por su empresa");
    public static final ErrorCode USER_NO_ACCESS_TO_COMPANY = ErrorCode.of(
                    "auth-011", "El usuario no tiene acceso a la empresa a la que intenta cambiar");
    public static ErrorCode TOKEN_INVALID = ErrorCode.of("auth-001", "El token Jwt Es Invalido");
    public static final ErrorCode VEHICLE_NOT_FOUND = ErrorCode.of("veh-001", "El vehículo no existe");
    public static final ErrorCode GEOFENCE_NOT_ASSOCIATED = ErrorCode.of("veh-002",
                    "La geocerca no está asociada al vehículo");
    public static final ErrorCode ROUTE_NOT_FOUND = ErrorCode.of("rts-001", "La ruta no existe");
    public static final ErrorCode ROUTE_NAME_EXISTS = ErrorCode.of("rts-002",
                    "Ya existe una ruta con el mismo nombre para esta empresa");
    public static final ErrorCode TRIP_NOT_FOUND = ErrorCode.of("dsp-001", "El viaje no existe");
    public static final ErrorCode DISPATCH_QUEUE_NOT_FOUND = ErrorCode.of("dsp-002",
                    "La entrada en la cola de despacho no existe");
    public static final ErrorCode VEHICLE_ALREADY_IN_QUEUE = ErrorCode.of("dsp-003",
                    "El vehículo ya se encuentra en la cola de despacho");
    public static final ErrorCode VEHICLE_ALREADY_ON_TRIP = ErrorCode.of("dsp-004",
                    "El vehículo ya tiene un viaje activo");
    public static final ErrorCode DISPATCH_CONCURRENT_CONFLICT = ErrorCode.of("dsp-005",
                    "Conflicto de concurrencia en el despacho — intente nuevamente");

    private String code;
    private String message;

    public static ErrorCode of(String code, String message) {
            return ErrorCode.builder().code(code).message(message).build();
    }
}
