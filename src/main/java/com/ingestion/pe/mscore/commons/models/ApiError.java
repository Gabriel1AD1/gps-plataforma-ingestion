package com.ingestion.pe.mscore.commons.models;

import com.ingestion.pe.mscore.commons.exception.BadRequest;
import com.ingestion.pe.mscore.commons.exception.ErrorCode;
import com.ingestion.pe.mscore.commons.exception.NotFound;
import com.ingestion.pe.mscore.commons.exception.Unauthorized;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@AllArgsConstructor
@ToString
@NoArgsConstructor
@Builder
public class ApiError {
  private static final Logger log = LoggerFactory.getLogger(ApiError.class);

  @JsonProperty("status")
  private HttpStatus status;

  @JsonProperty("timestamp")
  private LocalDateTime timestamp;

  private ErrorCode errorCode;

  @JsonProperty("messageType")
  private String message;

  @JsonProperty("path")
  private String path;

  @JsonProperty("view")
  private boolean view;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private String debugMessage;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private String stackTrace;

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private Object errors;

  public static ApiError internalServerError(Exception debugMessage) {
    StackTraceElement[] stackTrace = debugMessage.getStackTrace();
    int limit = Math.min(stackTrace.length, 500);
    StackTraceElement[] limitedStackTrace = Arrays.copyOf(stackTrace, limit);
    log.warn("Ah ocurrido un error inesperado {}", Arrays.stream(limitedStackTrace).toArray());

    return ApiError.builder()
        .errorCode(ErrorCode.INTERNAL_SERVER_ERROR)
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .timestamp(LocalDateTime.now())
        .message("Contact support")
        .stackTrace(Arrays.toString(limitedStackTrace))
        .debugMessage(debugMessage.getMessage())
        .build();
  }

  public static ApiError validationFailed(Map<String, String> errors) {
    return ApiError.builder()
        .errorCode(ErrorCode.VALIDATION_FAILED)
        .status(HttpStatus.BAD_REQUEST)
        .timestamp(LocalDateTime.now())
        .message("Validation failed")
        .errors(errors)
        .build();
  }

  public static ApiError badRequest(String message, boolean b, HttpServletRequest request) {
    return ApiError.builder()
        .errorCode(ErrorCode.BAD_REQUEST)
        .view(b)
        .path(request.getRequestURI() + getQueryAdd(request))
        .status(HttpStatus.BAD_REQUEST)
        .timestamp(LocalDateTime.now())
        .message(message)
        .build();
  }

  public static ApiError badRequest(BadRequest ex, boolean b, HttpServletRequest request) {
    return ApiError.builder()
        .view(b)
        .errorCode(ex.getErrorCode())
        .path(request.getRequestURI() + getQueryAdd(request))
        .status(HttpStatus.BAD_REQUEST)
        .timestamp(LocalDateTime.now())
        .message(ex.getMessage())
        .build();
  }

  public static ApiError unauthorized(String message, HttpServletRequest request, ErrorCode errorCode) {
    return ApiError.builder()
        .view(true)
        .errorCode(errorCode)
        .path(request.getRequestURI() + getQueryAdd(request))
        .status(HttpStatus.UNAUTHORIZED)
        .timestamp(LocalDateTime.now())
        .message(message)
        .build();
  }

  private static String getQueryAdd(HttpServletRequest request) {
    return request.getQueryString() != null ? "?" + request.getQueryString() : "";
  }

  public static ApiError httpMessageNotReadableException(String message) {
    return ApiError.builder()
        .errorCode(ErrorCode.BAD_REQUEST)
        .status(HttpStatus.BAD_REQUEST)
        .timestamp(LocalDateTime.now())
        .message(
            "No se a proporcionado un body o algo que es requerido no se esta enviando por favor revisar la request")
        .debugMessage(message)
        .build();
  }

  public static ApiError dataIntegrityViolation(String message, boolean view) {
    return ApiError.builder()
        .errorCode(ErrorCode.DATABASE_CONFLICT)
        .status(HttpStatus.CONFLICT)
        .view(view)
        .timestamp(LocalDateTime.now())
        .message(message)
        .errors(List.of("Duplicate entry"))
        .build();
  }

  public static ApiError methodNotSupported(String method) {
    return ApiError.builder()
        .errorCode(ErrorCode.METHOD_NOT_ALLOWED)
        .status(HttpStatus.METHOD_NOT_ALLOWED)
        .timestamp(LocalDateTime.now())
        .message("HTTP method not supported: " + method)
        .build();
  }

  public static ApiError tokenExpiredException(String message) {
    return ApiError.builder()
        .errorCode(ErrorCode.EXPIRATION_EXPIRED)
        .status(HttpStatus.BAD_REQUEST)
        .timestamp(LocalDateTime.now())
        .message(message)
        .build();
  }

  public static ApiError endpointNotFound(String uri) {
    return ApiError.builder()
        .errorCode(ErrorCode.ENDPOINT_NOT_FOUND)
        .status(HttpStatus.NOT_FOUND)
        .timestamp(LocalDateTime.now())
        .message("El endpoint '" + uri + "' no existe")
        .build();
  }

  public static ApiError notFound(NotFound ex, boolean b, HttpServletRequest request) {
    return ApiError.builder()
        .view(b)
        .errorCode(ex.getErrorCode())
        .path(request.getRequestURI() + "?" + request.getQueryString())
        .status(HttpStatus.NOT_FOUND)
        .timestamp(LocalDateTime.now())
        .message(ex.getMessage())
        .build();
  }

  public static ApiErrorBuilder builder() {
    return new ApiErrorBuilder().path("").view(false);
  }

  public static ApiError forbidden(String message) {
    return ApiError.builder()
        .errorCode(ErrorCode.FORBIDDEN)
        .view(true)
        .status(HttpStatus.FORBIDDEN)
        .timestamp(LocalDateTime.now())
        .message(message)
        .build();
  }

  public static ApiError unauthorized(Unauthorized ex, boolean b, HttpServletRequest request) {
    return ApiError.builder()
        .view(b)
        .errorCode(ex.getErrorCode())
        .path(request.getRequestURI() + getQueryAdd(request))
        .status(HttpStatus.UNAUTHORIZED)
        .timestamp(LocalDateTime.now())
        .message(ex.getMessage())
        .build();
  }
}
