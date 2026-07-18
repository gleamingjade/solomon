package com.example.solomon.feature.trial.domain.entity;

import org.springframework.data.domain.Persistable;

import com.example.solomon.common.infra.persistence.jpa.domain.entity.BaseTimeEntity;
import com.example.solomon.feature.member.domain.entity.Member;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class TrialMember extends BaseTimeEntity implements Persistable<TrialMemberId> {

    @EmbeddedId
    private TrialMemberId id;

    @MapsId("trialId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trial_id")
    private Trial trial;

    @MapsId("memberId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Turn turn;

    private Integer readSeq;

    public static TrialMember create(Trial trial, Member member, String nickname) {
        TrialMember trialMember = new TrialMember();

        trialMember.id = new TrialMemberId();

        trialMember.trial = trial;
        trialMember.member = member;
        trialMember.nickname = nickname;
        trialMember.turn = Turn.STAND_BY;

        return trialMember;
    }

    @Override
    public boolean isNew() {
        return super.createdAt == null;
    }

}