package com.example.solomon.common.adapter.in.web.security.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.jackson2.CoreJackson2Module;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.session.data.redis.config.annotation.SpringSessionRedisConnectionFactory;

import com.example.solomon.common.adapter.in.web.security.oauth.SessionMember;
import com.example.solomon.common.adapter.in.web.security.oauth.SessionMemberMixin;
import com.example.solomon.common.adapter.in.web.security.oauth.SessionRedisOidcUser;
import com.example.solomon.common.adapter.in.web.security.oauth.SessionRedisOidcUserMixin;

import java.util.List;

@Configuration
public class SessionRedisConfig {

    @Bean(name = "sessionRedisConnectionFactory")
    @SpringSessionRedisConnectionFactory
    public RedisConnectionFactory sessionRedisConnectionFactory(
            SessionRedisProperties props) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();

        config.setHostName(props.host());
        config.setPort(props.port());

        return new LettuceConnectionFactory(config);
    }

    // Since we have two RedisConnectionFactory beans, spring-session-data-redis
    // does not automatically create a RedisTemplate named "redisTemplate".
    // Therefore, we need to define it manually.
    @Bean(name = "redisTemplate")
    public RedisTemplate<Object, Object> redisTemplate(
            @Qualifier("sessionRedisConnectionFactory") RedisConnectionFactory connectionFactory) {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        return template;
    }

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        ObjectMapper mapper = new ObjectMapper();

        List<Module> securityModules = SecurityJackson2Modules.getModules(getClass().getClassLoader());
        mapper.registerModules(securityModules);
        mapper.registerModule(new CoreJackson2Module());

        mapper.addMixIn(SessionRedisOidcUser.class, SessionRedisOidcUserMixin.class);
        mapper.addMixIn(SessionMember.class, SessionMemberMixin.class);

        return new GenericJackson2JsonRedisSerializer(mapper);
    }

}
