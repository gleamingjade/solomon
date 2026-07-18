package com.example.solomon.feature.chat.domain.entity;

import java.util.UUID;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import com.github.f4b6a3.uuid.UuidCreator;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@PrimaryKeyClass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class ChatMessageKey {

    @PrimaryKeyColumn(name = "trial_id", type = PrimaryKeyType.PARTITIONED)
    private UUID trialId;

    @PrimaryKeyColumn(name = "id", type = PrimaryKeyType.CLUSTERED)
    private UUID id;

    public static ChatMessageKey create(UUID trialId) {
        return new ChatMessageKey(trialId, UuidCreator.getTimeOrderedEpoch());
    }

}