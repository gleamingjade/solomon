package com.example.solomon.feature.chat.adapter.in.web;

import java.util.UUID;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyQueryMetadata;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.solomon.common.adapter.in.web.dto.SuccessResponse;
import com.example.solomon.common.domain.exception.BusinessException;
import com.example.solomon.feature.chat.adapter.in.messaging.kafka.config.ChatKafkaStreamsConfig;
import com.example.solomon.feature.chat.adapter.in.web.dto.ChatConnectInfoResponse;
import com.example.solomon.feature.chat.domain.exception.ChatException;

import lombok.RequiredArgsConstructor;

// Session routing follows Kafka Streams' own task assignment instead of a separately-tracked
// Redis mapping (see "Chat Persistence: Kafka Streams State Store" wiki doc). Any instance can
// answer this - Streams shares task-assignment metadata across the whole consumer group - so
// there's no need to route this request to a specific "owning" instance first.
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatConnectionController {

    @Qualifier("chatMessageStreamsBuilder")
    private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;

    @GetMapping("/connect-info")
    public ResponseEntity<SuccessResponse<ChatConnectInfoResponse>> connectInfo(@RequestParam UUID trialId) {
        KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();

        KeyQueryMetadata metadata = kafkaStreams.queryMetadataForKey(
                ChatKafkaStreamsConfig.CHAT_MESSAGE_STORE_NAME, trialId.toString(), Serdes.String().serializer());

        if (metadata == null || metadata == KeyQueryMetadata.NOT_AVAILABLE) {
            throw new BusinessException(ChatException.NO_AVAILABLE_SERVER);
        }

        // application.server is set to "${SERVER_ID}:0" (see KafkaStreamsConfig) purely so this
        // host field carries the owning server's SERVER_ID - the port is a placeholder, never
        // dialed directly. Clients still connect via the existing /ws-{serverId} STOMP endpoint.
        String serverId = metadata.activeHost().host();

        return ResponseEntity.ok(SuccessResponse.of(new ChatConnectInfoResponse(serverId)));
    }

}
