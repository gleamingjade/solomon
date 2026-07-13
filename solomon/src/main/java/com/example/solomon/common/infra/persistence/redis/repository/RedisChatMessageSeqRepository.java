package com.example.solomon.common.infra.persistence.redis.repository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import com.example.solomon.feature.chat.domain.repository.ChatMessageSeqRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RedisChatMessageSeqRepository implements ChatMessageSeqRepository {

    private final RedisTemplate<String, String> chatMessageSeqRedisTemplate;

    @Override
    public Long incr(String trialId) {
        return chatMessageSeqRedisTemplate.opsForValue().increment(trialId);
    }

}
