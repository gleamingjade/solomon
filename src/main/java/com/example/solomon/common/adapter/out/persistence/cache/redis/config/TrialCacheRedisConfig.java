package com.example.solomon.common.adapter.out.persistence.cache.redis.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class TrialCacheRedisConfig {

    @Bean
    public RedisTemplate<String, String> trialCacheRedisTemplate(
            @Qualifier("redisConnectionFactory") RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();

        template.setConnectionFactory(factory);

        template.setKeySerializer(
                new StringRedisSerializer());

        template.setValueSerializer(
                new StringRedisSerializer());

        return template;
    }

}
