package com.example.solomon.common.config.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "redis.session")
public record SessionRedisProperties(String host, int port) {
}
