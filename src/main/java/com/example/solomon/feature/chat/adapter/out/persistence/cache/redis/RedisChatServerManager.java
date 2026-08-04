package com.example.solomon.feature.chat.adapter.out.persistence.cache.redis;

import com.example.solomon.feature.chat.application.out.ChatServerManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import static com.example.solomon.feature.chat.adapter.out.persistence.cache.redis.ServerRegistrationRunner.SERVERS_KEY;

@Component
@RequiredArgsConstructor
public class RedisChatServerManager implements ChatServerManager {

    private final RedisTemplate<String, String> trialCacheRedisTemplate;

    @Override
    public String allocate() {
         trialCacheRedisTemplate.opsForSet().members(SERVERS_KEY);
    }

}
