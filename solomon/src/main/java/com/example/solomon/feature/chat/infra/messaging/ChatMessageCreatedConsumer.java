package com.example.solomon.feature.chat.infra.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.solomon.common.app.dto.exception.AppException;
import com.example.solomon.common.infra.messaging.kafka.DebeziumEnvelope;
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

        // 웹소켓 fanout
        // db 업데이트
        Trial trial = trialRepository.findByIdWithTrialMembers(event.trialId())
                .orElseThrow(() -> new AppException(TrialException.UNEXISTS_TRIAL));

        trial.onNewChatMessage(event.content(), event.sequence());

        trial.getTrialMembers();

        // 메시지 오면 해당 방에 참여중인 유저 검색,
        // caffaine에 올림
        // fanout
        // last_message, last_message_seq
        // fanout

    }

    @KafkaListener(topics = "cdc-scylla.localscylla.chat_message", groupId = "websocket-fanout")
    public void fanout(DebeziumEnvelope envelope) {

    }

    @KafkaListener(topics = "cdc-scylla.localscylla.chat_message", groupId = "chat-db-updater")
    public void updateDb(DebeziumEnvelope envelope) {
        ChatMessageCreatedEvent event = ChatMessageCreatedEvent.from(envelope);

        trialRepository.findById(event.trialId())
                .orElseThrow(() -> new AppException(TrialException.UNEXISTS_TRIAL))
                .onNewChatMessage(event.content(), event.sequence());
    }
}
