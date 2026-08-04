package com.example.solomon.feature.chat.adapter.out.persistence.cache.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServerRegistrationRunner implements ApplicationRunner {

    private final RedisTemplate<String, String> trialCacheRedisTemplate;

    @Value("${SERVER_ID}")
    private String serverId;

    public static final String SERVERS_KEY = "servers";

    @Override
    public void run(ApplicationArguments args) {
        trialCacheRedisTemplate.opsForSet().add(SERVERS_KEY, serverId);
    }

}
