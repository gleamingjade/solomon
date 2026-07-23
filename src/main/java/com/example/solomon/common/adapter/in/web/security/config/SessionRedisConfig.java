package com.example.solomon.common.adapter.in.web.security.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.jackson2.CoreJackson2Module;
import org.springframework.security.jackson2.SecurityJackson2Modules;

import java.util.List;

@Configuration
public class SessionRedisConfig {

    @Bean(name = "sessionRedisConnectionFactory")
    public RedisConnectionFactory sessionRedisConnectionFactory(
            SessionRedisProperties props) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();

        config.setHostName(props.host());
        config.setPort(props.port());

        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        ObjectMapper mapper = new ObjectMapper();

        List<Module> securityModules = SecurityJackson2Modules.getModules(getClass().getClassLoader());
        mapper.registerModules(securityModules); //
        mapper.registerModule(new CoreJackson2Module());

        return new GenericJackson2JsonRedisSerializer(mapper);
    }

}
