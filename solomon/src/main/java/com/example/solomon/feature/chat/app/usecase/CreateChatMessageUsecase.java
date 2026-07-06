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

        // 채팅 웹소켓 발송용 애플리케이션 이벤트 발송(프레임워크에 묶이는 건 괜찮음 내가 봤을 때)
    }

}
