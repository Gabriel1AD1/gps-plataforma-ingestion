package com.ingestion.pe.mscore.clients.cache;

import com.ingestion.pe.mscore.commons.libs.auth.models.UserPrincipal;
import com.ingestion.pe.mscore.config.cache.RedisCacheDao;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthCache {

  private final RedisCacheDao<UserPrincipal> redisCacheDao;

  public Optional<UserPrincipal> getUserPrincipal(Long userId) {
    return redisCacheDao.get("auth:user_principal:" + userId, UserPrincipal.class);
  }
}
