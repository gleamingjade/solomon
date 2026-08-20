package com.example.solomon.feature.chat.adapter.out.persistence.cache.redis;

import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.example.solomon.common.domain.exception.BusinessException;
import com.example.solomon.feature.chat.application.out.ChatServerManager;
import com.example.solomon.feature.chat.domain.exception.ChatException;

import lombok.RequiredArgsConstructor;

import static com.example.solomon.feature.chat.adapter.out.persistence.cache.redis.ServerRegistrationRunner.SERVERS_KEY;
import static com.example.solomon.feature.chat.adapter.out.persistence.cache.redis.ServerRegistrationRunner.healthKey;

@Component
@RequiredArgsConstructor
public class RedisChatServerManager implements ChatServerManager {

    private final RedisTemplate<String, String> trialCacheRedisTemplate;

    @Override
    public String allocate(String trialId) {
        Set<String> serverIds = trialCacheRedisTemplate.opsForSet().members(SERVERS_KEY);

        if (serverIds == null || serverIds.isEmpty())
            throw new BusinessException(ChatException.NO_AVAILABLE_SERVER);

        String serverId = serverIds.stream()
                .filter(this::isAlive)
                .min((a, b) -> Long.compare(countOf(a), countOf(b)))
                .orElseThrow(() -> new BusinessException(ChatException.NO_AVAILABLE_SERVER));

        trialCacheRedisTemplate.opsForValue().increment(countKey(serverId));
        trialCacheRedisTemplate.opsForValue().set(trialServerKey(trialId), serverId);

        return serverId;
    }

    private boolean isAlive(String serverId) {
        return Boolean.TRUE.equals(trialCacheRedisTemplate.hasKey(healthKey(serverId)));
    }

    private long countOf(String serverId) {
        String count = trialCacheRedisTemplate.opsForValue().get(countKey(serverId));
        return count == null ? 0L : Long.parseLong(count);
    }

    private String countKey(String serverId) {
        return "server:" + serverId + ":count";
    }

    private String trialServerKey(String trialId) {
        return "trial:" + trialId + ":server";
    }

}
