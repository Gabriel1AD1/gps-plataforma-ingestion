package com.ingestion.pe.mscore.commons.exception;

import java.util.UUID;

public class AuthException {

  public static Unauthorized notAccessSystem() {
    Unauthorized badRequest = new Unauthorized("No eres un usuario valido del sistema");
    badRequest.setErrorCode(ErrorCode.NOT_ACCESS_SYSTEM);
    return badRequest;
  }

  public static Unauthorized sessionActive() {
    Unauthorized unauthorized =
        new Unauthorized("Usuario no autenticado o sesión no activa inicia sesión porfavor");
    unauthorized.setErrorCode(ErrorCode.USER_NOT_SESSION);
    return unauthorized;
  }

  public static BadRequest invalidCredentials() {
    BadRequest badRequest = new BadRequest("Credenciales inválidas");
    badRequest.setErrorCode(ErrorCode.CREDENTIALS_INVALID);
    return badRequest;
  }

  public static Unauthorized userNotFoundInCache() {
    Unauthorized unauthorized =
        new Unauthorized(
            "Tu usuario no se encontró en el servidor de sesiones intenta refrescar el token o volver a iniciar sesión porfavor");
    unauthorized.setErrorCode(ErrorCode.USER_NOT_FOUND_IN_SESSION_SERVER);
    return unauthorized;
  }

  public static Unauthorized userNotEqualsToCache() {
    Unauthorized unauthorized =
        new Unauthorized(
            "Tu usuario no coincide con el usuario de la sesión actual intenta refrescar el token o volver a iniciar sesión porfavor");
    unauthorized.setErrorCode(ErrorCode.USER_NOT_EQUALS_SESSION_SERVER);
    return unauthorized;
  }

  public static NotFound userNotFound(Long id) {
    NotFound notFound = new NotFound("Usuario no encontrado con el id: " + id);
    notFound.setErrorCode(ErrorCode.USER_NOT_FOUND);
    return notFound;
  }
}
