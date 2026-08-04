package com.example.solomon.feature.chat.adapter.out.persistence.cache.redis;

import com.example.solomon.feature.chat.application.out.ChatMessageSeqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
@RequiredArgsConstructor
public class RedisChatMessageSeqRepository implements ChatMessageSeqRepository {

    private final RedisTemplate<String, String> trialCacheRedisTemplate;

    private final String CHAT_SEQ_HKEY = "chat:seq";

    @Override
    public Long incr(String trialId) {
        return trialCacheRedisTemplate.opsForHash().increment(CHAT_SEQ_HKEY, trialId, 1);
    }

    @Override
    public void fillBack(String trialId, String seq) {
        trialCacheRedisTemplate.opsForHash().putAll(CHAT_SEQ_HKEY, Map.of(trialId, seq));
    }

}
