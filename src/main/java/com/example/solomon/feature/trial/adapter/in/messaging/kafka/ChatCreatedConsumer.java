package com.example.solomon.feature.trial.adapter.in.messaging.kafka;

import com.example.solomon.common.adapter.in.messaging.kafka.DebeziumEnvelope;
import com.example.solomon.feature.trial.application.out.TrialRepository;
import com.example.solomon.feature.trial.domain.entity.Trial;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatCreatedConsumer {

    private final TrialRepository trialRepository;

    private static final String FIELD_TRIAL_ID = "trial_id";
    private static final String FIELD_CONTENT = "content";
    private static final String FIELD_SEQUENCE = "sequence";

    @KafkaListener(topics = "cdc-scylla.localscylla.chat_message", containerFactory = "chatMessageCreatedConsumerFactory")
    public void consume(DebeziumEnvelope envelope) {
        Trial trial = trialRepository.findById(UUID.fromString(envelope.getValFromAfter(FIELD_TRIAL_ID).asText()))
                .orElseThrow(() -> new IllegalStateException("Trial does not exist"));

        trial.onNewChatMessage(
                envelope.getValFromAfter(FIELD_CONTENT).asText(),
                envelope.getValFromAfter(FIELD_SEQUENCE).asLong());

    }

}
