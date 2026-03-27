package com.ingestion.pe.mscore.config.redis;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingestion.pe.mscore.commons.libs.utils.JsonUtils;
import com.ingestion.pe.mscore.domain.monitoring.core.model.TripActiveResponse;
import com.ingestion.pe.mscore.clients.models.VehicleResponse;
import com.ingestion.pe.mscore.domain.atu.model.AtuTokenCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        ObjectMapper mapper = JsonUtils.serializerMapper.copy();
        mapper.addMixIn(TripActiveResponse.class, TripResponseMixIn.class);
        mapper.addMixIn(VehicleResponse.class, VehicleResponseMixIn.class);
        mapper.addMixIn(AtuTokenCache.class, AtuTokenCacheMixIn.class);

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(mapper);

        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, defaultImpl = TripActiveResponse.class)
    abstract static class TripResponseMixIn {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, defaultImpl = VehicleResponse.class)
    abstract static class VehicleResponseMixIn {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, defaultImpl = AtuTokenCache.class)
    abstract static class AtuTokenCacheMixIn {}
}

