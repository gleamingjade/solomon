package com.example.solomon.feature.chat.application.out;

import com.example.solomon.feature.chat.domain.event.ChatMessageCreatedEvent;

public interface ChatMessagePublisher {

    void publish(ChatMessageCreatedEvent event);

}
