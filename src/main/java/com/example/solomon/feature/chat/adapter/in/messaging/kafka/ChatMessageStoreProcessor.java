package com.example.solomon.feature.chat.adapter.in.messaging.kafka;

import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.solomon.feature.chat.adapter.in.messaging.kafka.config.ChatKafkaStreamsConfig;
import com.example.solomon.feature.chat.adapter.out.persistence.kafkastreams.ChatMessageStoreKey;
import com.example.solomon.feature.chat.application.out.ChatMessagePublisher;
import com.example.solomon.feature.chat.domain.event.ChatMessageCreatedEvent;

// Every server independently runs this topology and builds its own local copy of the state store
// (see ChatKafkaStreamsConfig), but Kafka Streams only ever calls process() on the *active* task
// instance for a given partition - standby instances replay the changelog to stay warm without
// ever invoking this. That's what lets fanout run unconditionally below, with no serverId
// ownership check (see the "Chat Persistence: Kafka Streams State Store" wiki doc).
//
// forward() is best-effort: it only feeds the derived-work topic (chat list preview), which
// tolerates occasional loss - the next message overwrites it. If forward() were allowed to throw,
// its failure would retry this whole record, including the already-succeeded, non-idempotent
// WebSocket fanout above it. See TODO-chat-derived-work-perf.md for why this forwards to a
// dedicated topic instead of reusing the state store's own changelog (for now).
public class ChatMessageStoreProcessor
        implements Processor<String, ChatMessageCreatedEvent, String, ChatMessageCreatedEvent> {

    private static final Logger log = LoggerFactory.getLogger(ChatMessageStoreProcessor.class);

    private final ChatMessagePublisher chatMessagePublisher;

    private ProcessorContext<String, ChatMessageCreatedEvent> context;

    private KeyValueStore<ChatMessageStoreKey, ChatMessageCreatedEvent> store;

    public ChatMessageStoreProcessor(ChatMessagePublisher chatMessagePublisher) {
        this.chatMessagePublisher = chatMessagePublisher;
    }

    @Override
    public void init(ProcessorContext<String, ChatMessageCreatedEvent> context) {
        this.context = context;
        this.store = context.getStateStore(ChatKafkaStreamsConfig.CHAT_MESSAGE_STORE_NAME);
    }

    @Override
    public void process(Record<String, ChatMessageCreatedEvent> record) {
        ChatMessageCreatedEvent event = record.value();

        store.put(new ChatMessageStoreKey(event.trialId(), event.sequence()), event);

        chatMessagePublisher.publish(event);

        try {
            context.forward(record);
        } catch (Exception e) {
            log.warn("Failed to forward chat message for derived work, trialId={}, sequence={}",
                    event.trialId(), event.sequence(), e);
        }
    }

}
