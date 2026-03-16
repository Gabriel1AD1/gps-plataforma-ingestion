package com.ingestion.pe.mscore.config.security;

import com.ingestion.pe.mscore.commons.exception.ErrorCode;
import com.ingestion.pe.mscore.commons.libs.utils.JsonUtils;
import com.ingestion.pe.mscore.commons.models.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException {

    response.setContentType("application/json");
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

    ApiError apiError = ApiError.unauthorized(
        "Token inválido o no proporcionado", request, ErrorCode.TOKEN_INVALID);

    response.getWriter().write(JsonUtils.toJson(apiError));
  }
}
