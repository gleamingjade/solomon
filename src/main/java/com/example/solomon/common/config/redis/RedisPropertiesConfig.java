package com.example.solomon.common.config.redis;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        BrokerRedisProperties.class,
        SessionRedisProperties.class
})
public class RedisPropertiesConfig {
}
