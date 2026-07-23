package com.example.solomon.feature.chat.domain.entity;

import java.util.UUID;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(value = "chat_message", keyspace = "localscylla")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class ChatMessage {

    @PrimaryKey
    private ChatMessageKey key;

    @Column("member_id")
    private Long memberId;

    private Long sequence;

    private String content;

    public static ChatMessage create(UUID trialId, Long memberId, Long sequence, String content) {
        return new ChatMessage(ChatMessageKey.create(trialId), memberId, sequence, content);
    }

}