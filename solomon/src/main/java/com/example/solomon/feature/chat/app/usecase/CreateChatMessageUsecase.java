package com.example.solomon.feature.chat.app.usecase;

import org.springframework.stereotype.Service;

import com.example.solomon.feature.chat.app.dto.CreateChatMessageCommand;
import com.example.solomon.feature.chat.domain.entity.ChatMessage;
import com.example.solomon.feature.chat.domain.repository.ChatMessageRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateChatMessageUsecase {

    private final ChatMessageRepository chatMessageRepository;

    public void execute(CreateChatMessageCommand command) {
        chatMessageRepository
                .save(ChatMessage.create(command.trialId(), command.memberId(), command.content()));

        // ChatMessageCreatedEvent publish
    }

}
