package com.example.solomon.common.adapter.out.persistence.cache.redis.config;

import com.example.solomon.common.adapter.in.web.security.config.SessionRedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        TrialCacheRedisProperties.class,
        SessionRedisProperties.class
})
public class RedisPropertiesConfig {
}
