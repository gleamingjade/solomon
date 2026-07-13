package com.example.solomon.feature.chat.application.in.usecase;

import org.springframework.stereotype.Service;

import com.example.solomon.feature.chat.application.in.usecase.dto.CreateChatMessageCommand;
import com.example.solomon.feature.chat.domain.entity.ChatMessage;
import com.example.solomon.feature.chat.domain.repository.ChatMessageRepository;
import com.example.solomon.feature.chat.domain.repository.ChatMessageSeqRepository;
import com.example.solomon.feature.chat.domain.service.ChatMessageSender;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateChatMessageUsecase {

    private final ChatMessageSender chatMessageSender;

    private final ChatMessageRepository chatMessageRepository;

    private final ChatMessageSeqRepository chatMessageSeqRepository;

    public void execute(CreateChatMessageCommand command) {
        Long seq = chatMessageSeqRepository.incr(command.trialId().toString());

        if (seq == 0L) {
            // fill back
        } else {
            chatMessageSender.fanout(chatMessageRepository
                    .save(ChatMessage.create(command.trialId(), command.memberId(), seq, command.content())));
        }
    }

}
