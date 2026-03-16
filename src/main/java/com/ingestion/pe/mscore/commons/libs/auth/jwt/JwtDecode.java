package com.ingestion.pe.mscore.commons.libs.auth.jwt;

import static com.auth0.jwt.algorithms.Algorithm.HMAC256;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.ingestion.pe.mscore.clients.cache.AuthCache;
import com.ingestion.pe.mscore.commons.exception.AuthException;
import com.ingestion.pe.mscore.commons.exception.CommonsException;
import com.ingestion.pe.mscore.commons.libs.auth.models.UserPrincipal;
import com.ingestion.pe.mscore.commons.models.enums.Role;
import com.ingestion.pe.mscore.commons.models.enums.Scopes;
import com.ingestion.pe.mscore.commons.models.enums.TimeZone;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtDecode {
  private static final Logger log = LoggerFactory.getLogger(JwtDecode.class);
  private final AuthCache authCache;

  @Value("${secret.token}")
  private String SECRET_KEY;

  private static Set<Scopes> getScopes(DecodedJWT decodedJWT) {
    Claim scopes = decodedJWT.getClaim("scopes");
    if (scopes == null || scopes.isNull()) {
      return Set.of();
    }
    List<String> scopesList = scopes.asList(String.class);
    if (scopesList == null) {
      return Set.of();
    }
    return scopesList.stream()
        .filter(Objects::nonNull)
        .map(Scopes::fromValue)
        .collect(Collectors.toSet());
  }

  public DecodedJWT decodeToken(String token) {
    DecodedJWT decodedJWT = JWT.require(HMAC256(SECRET_KEY)).build().verify(token);

    Instant expirationTime = Instant.ofEpochSecond(decodedJWT.getExpiresAt().getTime() / 1000);
    Instant currentTime = Instant.now();

    if (expirationTime.isBefore(currentTime)) {
      throw CommonsException.expirationExpired();
    }

    return decodedJWT;
  }

  public List<Role> extractRolesFromToken(DecodedJWT decodedJWT) {
    Claim roles = decodedJWT.getClaim("roles");
    if (roles == null || roles.isNull()) {
      return List.of();
    }
    List<String> rolesList = roles.asList(String.class);
    if (rolesList == null) {
      return List.of();
    }
    return rolesList.stream().filter(Objects::nonNull).map(Role::valueOf).toList();
  }

  public UserPrincipal getPrincipalFromToken(String token) {
    DecodedJWT decodedJWT = decodeToken(token);
    UserPrincipal userPrincipal = createUserPrincipal(decodedJWT);

    return authCache
        .getUserPrincipal(userPrincipal.getId())
        .orElseThrow(AuthException::userNotFoundInCache);
  }

  public UserPrincipal createUserPrincipal(DecodedJWT decodedJWT) {
    log.debug("Autenticación por usuario");
    Long userId = decodedJWT.getClaim("id").asLong();
    UUID uuid = UUID.fromString(decodedJWT.getClaim("uuid").asString());
    String email = decodedJWT.getClaim("email").asString();
    TimeZone timeZone = TimeZone.valueOf(decodedJWT.getClaim("time_zone").asString());
    List<Role> roles = extractRolesFromToken(decodedJWT);
    Long companyId = decodedJWT.getClaim("company_id").asLong();
    Set<Scopes> scopes = getScopes(decodedJWT);
    return UserPrincipal.builder()
        .companyId(companyId)
        .id(userId)
        .email(email)
        .uuid(uuid)
        .roles(roles)
        .scopes(scopes)
        .timeZone(timeZone)
        .build();
  }
}
