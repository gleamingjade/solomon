package com.example.solomon.common.adapter.in.web.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "redis.session")
public record SessionRedisProperties(String host, int port) {
}
