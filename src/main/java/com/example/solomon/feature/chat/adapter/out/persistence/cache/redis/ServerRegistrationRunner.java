package com.example.solomon.feature.chat.adapter.out.persistence.cache.redis;

import java.time.Duration;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ServerRegistrationRunner implements ApplicationRunner {

    private final RedisTemplate<String, String> trialCacheRedisTemplate;

    @Value("${SERVER_ID}")
    private String serverId;

    public static final String SERVERS_KEY = "servers";

    private static final Duration HEALTH_TTL = Duration.ofSeconds(10);

    @Override
    public void run(ApplicationArguments args) {
        trialCacheRedisTemplate.opsForSet().add(SERVERS_KEY, serverId);
    }

    @Scheduled(fixedRate = 9000)
    public void heartbeat() {
        trialCacheRedisTemplate.opsForValue().set(healthKey(serverId), "alive", HEALTH_TTL);
    }

    public static String healthKey(String serverId) {
        return "server:health:" + serverId;
    }

}
