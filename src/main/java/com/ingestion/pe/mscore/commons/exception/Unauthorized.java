package com.ingestion.pe.mscore.commons.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Unauthorized extends RuntimeException {
  private ErrorCode errorCode;

  public Unauthorized(String message) {
    super(message);
  }

  public Unauthorized(String message, ErrorCode errorCode) {
    super(message);
    this.errorCode = errorCode;
  }

  public static Unauthorized of(ErrorCode errorCode, String message) {
    return new Unauthorized(message, errorCode);
  }
}
