package com.example.solomon.feature.chat.adapter.out.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.example.solomon.feature.chat.application.out.ChatMessagePublisher;
import com.example.solomon.feature.chat.domain.event.ChatMessageCreatedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StompChatMessagePublisher implements ChatMessagePublisher {

    private final SimpMessagingTemplate simpMessagingTemplate;

    @Override
    public void publish(ChatMessageCreatedEvent event) {
        simpMessagingTemplate.convertAndSend("/topic/chat/" + event.trialId(), event);
    }

}