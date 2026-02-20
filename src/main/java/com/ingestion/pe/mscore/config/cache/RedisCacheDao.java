package com.ingestion.pe.mscore.config.cache;

import com.ingestion.pe.mscore.commons.libs.utils.JsonUtils;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisCacheDao<T> implements CacheDao<T> {

    private final RedisTemplate<String, Object> redis;

    @Override
    public void save(String key, T value, long ttlSeconds) {
        redis.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public Optional<T> get(String key, Class<T> type) {
        Object value = redis.opsForValue().get(key);
        if (value == null) {
            return Optional.empty();
        }

        if (type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }

        try {
            T converted = JsonUtils.deserializerMapper.convertValue(value, type);
            return Optional.of(converted);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                    "Error converting Redis value for key=" + key + " to " + type.getSimpleName(),
                    e);
        }
    }

    @Override
    public void delete(String key) {
        redis.delete(key);
    }

    @Override
    public boolean exists(String key) {
        return redis.hasKey(key);
    }
}
