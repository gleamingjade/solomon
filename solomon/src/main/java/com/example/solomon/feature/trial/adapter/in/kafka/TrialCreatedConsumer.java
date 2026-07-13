package com.example.solomon.feature.trial.adapter.in.kafka;

import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.solomon.common.infra.messaging.kafka.DebeziumEnvelope;
import com.example.solomon.feature.chat.app.dto.CreateChatMessageCommand;
import com.example.solomon.feature.chat.app.usecase.CreateChatMessageUsecase;
import com.example.solomon.feature.trial.domain.entity.TrialMember;
import com.example.solomon.feature.trial.domain.repository.TrialMemberRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TrialCreatedConsumer {

    private final TrialMemberRepository trialMemberRepository;

    private final CreateChatMessageUsecase createChatMessageUsecase;

    @KafkaListener(topics = "cdc-mysql.localdb.trial-created", containerFactory = "trialCreatedConsumerFactory")
    public void consume(DebeziumEnvelope envelope) {
        UUID trialId = UUID.fromString(envelope.payload().after().get("id").asText());

        TrialMember creator = trialMemberRepository.findAllByTrialId(trialId).get(0);

        createChatMessageUsecase.execute(
                new CreateChatMessageCommand(
                        trialId,
                        null,
                        creator.getNickname() + "came into play."));
    }

}
