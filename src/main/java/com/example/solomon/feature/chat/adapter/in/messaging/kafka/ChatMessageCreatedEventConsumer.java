package com.example.solomon.feature.chat.adapter.in.messaging.kafka;

import com.example.solomon.feature.chat.adapter.in.messaging.kafka.config.ChatKafkaTopicConfig;
import com.example.solomon.feature.chat.application.out.ChatMessagePublisher;
import com.example.solomon.feature.chat.domain.event.ChatMessageCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatMessageCreatedEventConsumer {

    private final ChatMessagePublisher chatMessagePublisher;

    @KafkaListener(topics = ChatKafkaTopicConfig.CHAT_MESSAGE_CREATED_EVENT, containerFactory = "chatMessageCreatedEventConsumerFactory")
    public void consume(ChatMessageCreatedEvent event) {
        chatMessagePublisher.publish(event);
    }

}
