package com.example.solomon.common.infra.websocket;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.example.solomon.feature.chat.domain.entity.ChatMessage;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebSocketPublisher {

    private final RedisTemplate<String, Object> chatRedisTemplate;

    // TrialCreatedEvent 받아서 redis pub/sub에 시스템 메시지 pub
    public void publishSystemMessage() {
        chatRedisTemplate.convertAndSend(
                "ㅇㅇㅇㅇ",
                "message");
    }

    // ChatMessageCreatedEvent 받아서 redis pub/sub에 채팅 pub
    public void publishChatMessage(ChatMessage message) {
        chatRedisTemplate.convertAndSend(
                "ㅇㅇㅇㅇ",
                message);
    }

    // redis sub 후 채팅방 멤버 fanout
    public void send() {
    }

}
