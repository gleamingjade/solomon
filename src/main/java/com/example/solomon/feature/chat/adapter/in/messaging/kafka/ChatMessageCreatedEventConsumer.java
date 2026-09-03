package com.example.solomon.feature.chat.adapter.in.messaging.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.solomon.common.domain.exception.BusinessException;
import com.example.solomon.feature.chat.adapter.in.messaging.kafka.config.ChatKafkaTopicConfig;
import com.example.solomon.feature.chat.adapter.out.persistence.rocksdb.RocksDbChatMessageStore;
import com.example.solomon.feature.chat.application.out.ChatMessagePublisher;
import com.example.solomon.feature.chat.domain.event.ChatMessageCreatedEvent;
import com.example.solomon.feature.trial.application.out.TrialRepository;
import com.example.solomon.feature.trial.domain.entity.Trial;
import com.example.solomon.feature.trial.domain.exception.TrialException;

import lombok.RequiredArgsConstructor;

// Every chat server subscribes to this topic and applies to its own local RocksDB (redundancy,
// see "Chat Persistence" wiki doc for the neighbor-replication plan). Only the trial's owning
// server (event.serverId() == our own SERVER_ID) does the rest - fanout and derived work must
// only happen once, and ownership is already the single natural place to pin that to.
//
// If the RocksDB write throws, this method throws too, so the offset is never committed for
// this record - KafkaConsumerFactorySupport's error handler retries it, then dead-letters it if
// it keeps failing. Fanout/derived work only run after the RocksDB write already succeeded, so
// they can never fire on a record that's about to be retried.
@Component
@RequiredArgsConstructor
public class ChatMessageCreatedEventConsumer {

    private final RocksDbChatMessageStore rocksDbChatMessageStore;

    private final ChatMessagePublisher chatMessagePublisher;

    private final TrialRepository trialRepository;

    @Value("${SERVER_ID}")
    private String serverId;

    @KafkaListener(topics = ChatKafkaTopicConfig.CHAT_MESSAGE_CREATED_EVENT, containerFactory = "chatMessageCreatedEventConsumerFactory")
    public void consume(ChatMessageCreatedEvent event) {
        rocksDbChatMessageStore.put(event);

        if (!serverId.equals(event.serverId())) {
            return;
        }

        chatMessagePublisher.publish(event);

        Trial trial = trialRepository.findById(event.trialId())
                .orElseThrow(() -> new BusinessException(TrialException.UNEXISTS_TRIAL));

        trial.onNewChatMessage(event.content(), event.sequence());
        trialRepository.save(trial);
    }

}
