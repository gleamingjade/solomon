package com.example.solomon.common.adapter.out.persistence.cache.redis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "redis")
public record RedisProperties(String host, int port) {
}
