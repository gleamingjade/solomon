package com.example.solomon.feature.trial.adapter.in.messaging.kafka;

import com.example.solomon.common.domain.exception.BusinessException;
import com.example.solomon.feature.chat.adapter.in.messaging.kafka.config.ChatKafkaTopicConfig;
import com.example.solomon.feature.chat.domain.event.ChatMessageCreatedEvent;
import com.example.solomon.feature.trial.application.out.TrialRepository;
import com.example.solomon.feature.trial.domain.entity.Trial;
import com.example.solomon.feature.trial.domain.exception.TrialException;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component("trialChatMessageCreatedEventConsumer")
@RequiredArgsConstructor
public class ChatMessageCreatedEventConsumer {

    private final TrialRepository trialRepository;

    @KafkaListener(topics = ChatKafkaTopicConfig.CHAT_MESSAGE_CREATED_EVENT, containerFactory = "chatMessageCreatedEventTrialConsumerFactory")
    public void consume(ChatMessageCreatedEvent event) {
        Trial trial = trialRepository.findById(event.trialId())
                .orElseThrow(() -> new BusinessException(TrialException.UNEXISTS_TRIAL));

        trial.onNewChatMessage(event.content(), event.sequence());
    }

}
