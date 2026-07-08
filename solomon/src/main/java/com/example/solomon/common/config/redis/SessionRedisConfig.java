package com.example.solomon.common.config.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
public class SessionRedisConfig {

        @Bean(name = "sessionRedisConnectionFactory")
        public RedisConnectionFactory brokerRedisConnectionFactory(
                        SessionRedisProperties props) {
                RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();

                config.setHostName(props.host());
                config.setPort(props.port());

                return new LettuceConnectionFactory(config);
        }

}
