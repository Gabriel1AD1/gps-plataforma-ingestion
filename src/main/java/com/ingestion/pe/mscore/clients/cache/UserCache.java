package com.ingestion.pe.mscore.clients.cache;

import com.ingestion.pe.mscore.clients.models.UserResponse;
import com.ingestion.pe.mscore.config.cache.CacheDao;
import com.ingestion.pe.mscore.domain.auth.core.entity.UserEntity;
import com.ingestion.pe.mscore.domain.auth.core.repo.UserEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;


@Slf4j
@Component
@RequiredArgsConstructor
public class UserCache {

  private static final long TTL_SECONDS = 600; // 10 min

  private final UserEntityRepository userEntityRepository;
  private final CacheDao<UserResponse> cacheDao;

  private String key(Long userId) {
    return "user:" + userId;
  }

  /**
   * Obtiene usuarios por ID intentando primero en cache y luego en DB.
   */
  public List<UserResponse> getUsersByIds(Collection<Long> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return List.of();
    }

    return userIds.stream()
      .map(this::getById)
      .filter(java.util.Objects::nonNull)
      .toList();
  }

  /**
   * Obtiene un user por ID (cache + fallback DB compartida)
   */
  public UserResponse getById(Long userId) {
    return cacheDao.get(key(userId), UserResponse.class)
      .orElseGet(() -> {
        UserResponse loaded =
          userEntityRepository.findById(userId)
            .map(this::toResponse)
            .orElse(null);

        if (loaded != null) {
          cacheDao.save(
            key(userId),
            loaded,
            TTL_SECONDS
          );
        }

        return loaded;
      });
  }

  private UserResponse toResponse(UserEntity entity) {
    return UserResponse.builder()
      .id(entity.getId())
      .uuid(entity.getUuid())
      .email(entity.getEmail())
      .timeZone(entity.getTimeZone())
      .pushTokens(Set.of())
      .phones(entity.getPhones())
      .emails(entity.getEmails())
      .build();
  }
}
