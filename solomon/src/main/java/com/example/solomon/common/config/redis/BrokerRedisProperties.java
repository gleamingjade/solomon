package com.example.solomon.common.config.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "redis.broker")
public record BrokerRedisProperties(String host, int port) {
}
