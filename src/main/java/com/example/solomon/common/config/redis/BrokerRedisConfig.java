package com.example.solomon.common.config.redis;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class BrokerRedisConfig {

    @Bean(name = "brokerRedisConnectionFactory")
    public RedisConnectionFactory brokerRedisConnectionFactory(
            BrokerRedisProperties props) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();

        config.setHostName(props.host());
        config.setPort(props.port());

        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, String> chatMessageSeqRedisTemplate(
            @Qualifier("brokerRedisConnectionFactory") RedisConnectionFactory factory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();

        template.setConnectionFactory(factory);

        template.setKeySerializer(
                new StringRedisSerializer());

        template.setValueSerializer(
                new StringRedisSerializer());

        return template;
    }

}
