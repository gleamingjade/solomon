package com.example.solomon.feature.chat.infra.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.solomon.common.domain.entity.exception.ApplicationException;
import com.example.solomon.common.adapter.in.messaging.kafka.DebeziumEnvelope;
import com.example.solomon.common.adapter.in.messaging.kafka.config.KafkaTopics;
import com.example.solomon.feature.trial.adapter.out.persistence.jpa.SpringDataJpaTrialRepository;
import com.example.solomon.feature.trial.domain.exception.TrialException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatMessageCreatedConsumer {

    SpringDataJpaTrialRepository trialRepository;

    @KafkaListener(topics = KafkaTopics.CDC_SCYLLA_CHAT_MESSAGE, containerFactory = "chatMessageCreatedConsumerFactory")
    public void consume(DebeziumEnvelope envelope) {
        ChatMessageCreatedEvent event = ChatMessageCreatedEvent.from(envelope);


    }

    @KafkaListener(topics = KafkaTopics.CDC_SCYLLA_CHAT_MESSAGE, groupId = "websocket-fanout")
    public void fanout(DebeziumEnvelope envelope) {

    }

    @KafkaListener(topics = KafkaTopics.CDC_SCYLLA_CHAT_MESSAGE, groupId = "chat-db-updater")
    public void updateDb(DebeziumEnvelope envelope) {
        ChatMessageCreatedEvent event = ChatMessageCreatedEvent.from(envelope);

        trialRepository.findById(event.trialId())
                .orElseThrow(() -> new ApplicationException(TrialException.UNEXISTS_TRIAL))
                .onNewChatMessage(event.content(), event.sequence());
    }
}
