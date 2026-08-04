package com.example.solomon.feature.trial.domain.entity;

import java.util.ArrayList;
import java.util.List;

import com.example.solomon.common.domain.entity.jpa.UuidBaseEntity;
import com.example.solomon.common.domain.exception.BusinessException;
import com.example.solomon.feature.member.domain.entity.Member;

import com.example.solomon.feature.trial.domain.exception.TrialException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Trial extends UuidBaseEntity {

    public static final String AGGREGATE_TYPE = "Trial";

    @Column(nullable = false)
    private String issueTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Stage stage;

    private String lastMessage;

    private Long lastMessageSeq;

    @OneToMany(mappedBy = "trial", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrialMember> trialMembers = new ArrayList<>();

    public static Trial create(Member member, String issueTitle, String nickname) {
        Trial tr = new Trial();
        tr.issueTitle = issueTitle;
        tr.stage = Stage.STAND_BY;

        TrialMember tm = TrialMember.create(tr, member, nickname);
        tr.trialMembers.add(tm);

        return tr;
    }

    public void onNewChatMessage(String lastMessage, Long lastMessageSeq) {
        this.lastMessage = lastMessage;
        this.lastMessageSeq = lastMessageSeq;
    }

    private void validateIfJoinable(String nickname) {
        if (trialMembers.size() != 1) {
            throw new BusinessException(TrialException.CAPACITY_EXCEEDED);
        } else if (trialMembers.stream().anyMatch((tm) -> tm.getNickname().equals(nickname))) {
            throw new BusinessException(TrialException.DUPLICATED_NICKNAME);
        }
    }

    public void join(Member member, String nickname) {
        validateIfJoinable(nickname);
        TrialMember tm = TrialMember.create(this, member, nickname);
        this.trialMembers.add(tm);
    }

}