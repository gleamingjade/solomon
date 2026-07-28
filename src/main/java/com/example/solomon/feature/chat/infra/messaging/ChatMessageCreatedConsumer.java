package com.example.solomon.feature.chat.infra.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.solomon.common.domain.entity.exception.ApplicationException;
import com.example.solomon.common.adapter.in.messaging.kafka.DebeziumEnvelope;
import com.example.solomon.feature.trial.adapter.out.persistence.jpa.SpringDataJpaTrialRepository;
import com.example.solomon.feature.trial.domain.entity.Trial;
import com.example.solomon.feature.trial.domain.exception.TrialException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatMessageCreatedConsumer {

    SpringDataJpaTrialRepository trialRepository;

    @KafkaListener(topics = "cdc-scylla.localscylla.chat_message", containerFactory = "chatMessageCreatedConsumerFactory")
    public void consume(DebeziumEnvelope envelope) {
        ChatMessageCreatedEvent event = ChatMessageCreatedEvent.from(envelope);


    }

    @KafkaListener(topics = "cdc-scylla.localscylla.chat_message", groupId = "websocket-fanout")
    public void fanout(DebeziumEnvelope envelope) {

    }

    @KafkaListener(topics = "cdc-scylla.localscylla.chat_message", groupId = "chat-db-updater")
    public void updateDb(DebeziumEnvelope envelope) {
        ChatMessageCreatedEvent event = ChatMessageCreatedEvent.from(envelope);

        trialRepository.findById(event.trialId())
                .orElseThrow(() -> new ApplicationException(TrialException.UNEXISTS_TRIAL))
                .onNewChatMessage(event.content(), event.sequence());
    }
}
