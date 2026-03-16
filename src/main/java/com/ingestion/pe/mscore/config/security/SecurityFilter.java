package com.ingestion.pe.mscore.config.security;

import com.ingestion.pe.mscore.commons.exception.Unauthorized;
import com.ingestion.pe.mscore.commons.libs.auth.jwt.JwtDecode;
import com.ingestion.pe.mscore.commons.libs.auth.models.UserPrincipal;
import com.ingestion.pe.mscore.commons.libs.utils.JsonUtils;
import com.ingestion.pe.mscore.commons.models.ApiError;
import com.ingestion.pe.mscore.config.log.LogManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@AllArgsConstructor
@Component
@Slf4j
public class SecurityFilter extends OncePerRequestFilter {

  private final JwtDecode jwtDecode;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
        filterChain.doFilter(request, response);
        return;
    }

    doBearerFilter(request, response, filterChain);
  }

  private void doBearerFilter(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws IOException, ServletException {

    String requestId = Optional.ofNullable(request.getHeader("X-Request-ID")).orElse(UUID.randomUUID().toString());
    response.setHeader("X-Request-ID", requestId);

    LogManager.addRequestId(requestId);
    LogManager.addPath(request.getRequestURI());
    LogManager.addClientIp(getClientIpAddress(request));
    LogManager.addApplicationName("ingestion");

    Optional<String> authorizationHeader = Optional.ofNullable(request.getHeader("Authorization"));

    if (authorizationHeader.isPresent() && authorizationHeader.get().startsWith("Bearer ")) {
      String bearerToken = authorizationHeader.get().substring(7);
      try {
        UserPrincipal userPrincipal = jwtDecode.getPrincipalFromToken(bearerToken);

        if (userPrincipal != null) {
          LogManager.addCompanyId(userPrincipal.getCompanyId().toString());
          LogManager.addUserId(userPrincipal.getId().toString());
          
          userPrincipal.setRequestId(requestId);
          List<GrantedAuthority> authorities = new ArrayList<>();

          authorities.addAll(
              userPrincipal.getRoles().stream()
                  .map(role -> new SimpleGrantedAuthority(role.name()))
                  .toList());

          authorities.addAll(
              userPrincipal.getScopes().stream()
                  .map(scope -> new SimpleGrantedAuthority(scope.value()))
                  .toList());

          UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userPrincipal,
              null, authorities);
          authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authentication);
          
          userPrincipal.setIpAddress(getClientIpAddress(request));
        }
      } catch (Unauthorized ex) {
        log.error("Error de autorización: {}", ex.getMessage());
        sendErrorResponse(response, request, ex);
        return;
      } catch (Exception e) {
        log.error("Error al procesar el token JWT: {}", e.getMessage());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return;
      }
    }
    filterChain.doFilter(request, response);
  }

  private void sendErrorResponse(HttpServletResponse response, HttpServletRequest request, Unauthorized ex) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json");
    response.getWriter().write(JsonUtils.toJson(ApiError.unauthorized(ex.getMessage(), request, ex.getErrorCode())));
  }

  private String getClientIpAddress(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isBlank()) {
      return xForwardedFor.split(",")[0].trim();
    }
    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isBlank()) {
      return xRealIp;
    }
    return request.getRemoteAddr();
  }
}
