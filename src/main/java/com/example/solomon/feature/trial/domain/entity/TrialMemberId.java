package com.example.solomon.feature.trial.domain.entity;

import java.io.Serializable;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class TrialMemberId implements Serializable {

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "trial_id", length = 16, nullable = false)
    private UUID trialId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

}