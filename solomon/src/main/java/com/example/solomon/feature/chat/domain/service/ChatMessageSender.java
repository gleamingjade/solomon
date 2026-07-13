package com.example.solomon.feature.chat.domain.service;

import com.example.solomon.feature.chat.domain.entity.ChatMessage;

public interface ChatMessageSender {

    public void fanout(ChatMessage chatMessage);

}
