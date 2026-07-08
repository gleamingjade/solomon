package com.example.solomon.common.infra.websocket;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.solomon.feature.chat.infra.messaging.event.ChatMessageCreatedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebSocketPublisher {

    private final SimpMessageSendingOperations simpMessageSendingOperations;

    private final RedisTemplate<String, Object> chatRedisTemplate;

    // @formatter:off
    // If the application layer, which contains business logic, depends on the infrastructure layer,
    // such as WebSocketPublisher, the application logic becomes coupled to infrastructure concerns,
    // losing focus on the business logic itself.
    //
    // To prevent this kind of situation, raise an event and let the infrastructure layer depend on the event
    // instead of the application layer depending on infrastructure concerns.
    //
    // I'm going to use Spring's ApplicationEventPublisher.
    // One concern is that the application layer becomes coupled to the framework.
    // However, I think this is acceptable since framework changes are rarely required in practice.
    // Since I'm using JPA, I decided to use JPA entities as domain objects instead of keeping a pure POJO-based domain model.
    // @formatter:on
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishChatMessage(ChatMessageCreatedEvent event) {
        chatRedisTemplate.convertAndSend(
                "chat.message.created",
                event);
    }

    // redis sub 후 해당 채팅방에 참여중인 member 검색해서 fanout
    public void send() {
    }

}
