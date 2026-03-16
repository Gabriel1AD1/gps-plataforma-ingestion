package com.ingestion.pe.mscore.commons.exception;

public class CommonsException {
  public static BadRequest expirationExpired() {
    BadRequest badRequest = new BadRequest("El tiempo de expiración ha expirado");
    badRequest.setErrorCode(ErrorCode.EXPIRATION_EXPIRED);
    return badRequest;
  }
}
