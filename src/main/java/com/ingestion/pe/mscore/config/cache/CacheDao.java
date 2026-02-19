package com.ingestion.pe.mscore.config.cache;

import java.util.Optional;

public interface CacheDao<T> {

    void save(String key, T value, long ttlSeconds);

    Optional<T> get(String key, Class<T> type);

    void delete(String key);

    boolean exists(String key);
}
