
package com.example.solomon.feature.chat.application.in.usecase;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.solomon.feature.chat.application.in.usecase.dto.CreateChatMessageCommand;
import com.example.solomon.feature.chat.application.out.ChatMessageRepository;
import com.example.solomon.feature.chat.application.out.ChatMessageSeqRepository;
import com.example.solomon.feature.chat.domain.entity.ChatMessage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateChatMessageUsecase {

    private final ChatMessageRepository chatMessageRepository;

    private final ChatMessageSeqRepository chatMessageSeqRepository;

    public void execute(CreateChatMessageCommand command) {
        execute(List.of(command));
    }

    // All commands must share the same trialId - saveAllInBatch only guarantees atomicity within
    // a single Scylla partition (see ChatMessageRepository).
    public void execute(List<CreateChatMessageCommand> commands) {
        List<ChatMessage> chatMessages = commands.stream()
                .map(this::toChatMessage)
                .toList();

        chatMessageRepository.saveAllInBatch(chatMessages);
    }

    private ChatMessage toChatMessage(CreateChatMessageCommand command) {
        Long seq = chatMessageSeqRepository.incr(command.trialId().toString());

        if (seq == 0L) {
            Optional<ChatMessage> cm = chatMessageRepository.findLatestByTrialId(command.trialId());

            if (cm.isPresent()) {
                seq = cm.get().getSequence();
                chatMessageSeqRepository.fillBack(command.trialId().toString(), String.valueOf(seq));
            }
        }

        return ChatMessage.create(command.trialId(), command.memberId(), seq, command.content(), command.type());
    }

}
