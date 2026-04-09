package com.ingestion.pe.mscore.commons.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotFound extends RuntimeException {
  private ErrorCode errorCode;

  public NotFound(String message) {
    super(message);
  }

  public NotFound(String message, ErrorCode cause) {
    super(message);
    errorCode = cause;
  }
}
