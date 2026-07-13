package com.example.solomon.common.infra.websocket;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.solomon.feature.chat.infra.messaging.ChatMessageCreatedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebSocketPublisher {

    // private final SimpMessageSendingOperations simpMessageSendingOperations;

    // private final RedisTemplate<String, Object> chatRedisTemplate;

    // @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    // public void publishChatMessage(ChatMessageCreatedEvent event) {
    //     chatRedisTemplate.convertAndSend(
    //             "chat.message.created",
    //             event);
    // }

    // // redis sub 후 해당 채팅방에 참여중인 member 검색해서 fanout
    // public void send() {
    // }

}
