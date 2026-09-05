package com.example.solomon.feature.chat.application.in.usecase;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.example.solomon.feature.chat.adapter.in.messaging.kafka.config.ChatKafkaTopicConfig;
import com.example.solomon.feature.chat.application.in.usecase.dto.CreateChatMessageCommand;
import com.example.solomon.feature.chat.application.out.ChatMessageSeqRepository;
import com.example.solomon.feature.chat.domain.event.ChatMessageCreatedEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateChatMessageUsecase {

    private final ChatMessageSeqRepository chatMessageSeqRepository;

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    @Qualifier("chatMessageTransactionalKafkaTemplate")
    private final KafkaTemplate<Object, Object> transactionalKafkaTemplate;

    @Value("${SERVER_ID}")
    private String serverId;

    // A single message has nothing to be atomic with, so this stays on the plain template -
    // no transaction round trips to pay for.
    public void execute(CreateChatMessageCommand command) {
        produce(kafkaTemplate, toEvent(command));
    }

    // Wrapped in one Kafka transaction so the batch is all-or-nothing - without this, one
    // message could land while another silently failed (e.g. the "joined" system message
    // publishes but the "ready" prompt doesn't), and re-deriving sequence numbers on a naive
    // retry would duplicate the one that already succeeded. See the "Chat Persistence" wiki doc.
    public void execute(List<CreateChatMessageCommand> commands) {
        List<ChatMessageCreatedEvent> events = commands.stream()
                .map(this::toEvent)
                .toList();

        transactionalKafkaTemplate.executeInTransaction(ops -> {
            events.forEach(event -> produce(ops, event));
            return null;
        });
    }

    private ChatMessageCreatedEvent toEvent(CreateChatMessageCommand command) {
        return new ChatMessageCreatedEvent(
                command.trialId(), command.content(),
                chatMessageSeqRepository.next(command.trialId()),
                command.type(),
                serverId);
    }

    private void produce(KafkaOperations<Object, Object> operations, ChatMessageCreatedEvent event) {
        operations.send(
                ChatKafkaTopicConfig.CHAT_MESSAGE_CREATED_EVENT,
                event.trialId().toString(),
                event);
    }

}
