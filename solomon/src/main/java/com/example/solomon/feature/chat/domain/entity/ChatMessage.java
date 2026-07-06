package com.example.solomon.feature.chat.domain.entity;

import java.util.UUID;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table("chat_message")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class ChatMessage {

    @PrimaryKey
    private ChatMessageKey key;

    private Long memberId;

    private String content;

    public static ChatMessage create(UUID trialId, Long memberId, String content) {
        return new ChatMessage(ChatMessageKey.create(trialId), memberId, content);
    }

}