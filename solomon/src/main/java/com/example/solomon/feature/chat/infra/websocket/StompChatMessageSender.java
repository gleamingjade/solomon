package com.example.solomon.feature.chat.infra.websocket;

import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

import com.example.solomon.feature.chat.domain.entity.ChatMessage;
import com.example.solomon.feature.chat.domain.service.ChatMessageSender;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StompChatMessageSender implements ChatMessageSender {

    private final SimpMessageSendingOperations simpMessageSendingOperations;

    @Override
    public void fanout(ChatMessage chatMessage) {

    }

}
