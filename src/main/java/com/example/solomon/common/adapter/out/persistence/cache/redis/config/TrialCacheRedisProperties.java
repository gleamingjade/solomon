package com.example.solomon.common.adapter.out.persistence.cache.redis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "redis.broker")
public record TrialCacheRedisProperties(String host, int port) {
}
