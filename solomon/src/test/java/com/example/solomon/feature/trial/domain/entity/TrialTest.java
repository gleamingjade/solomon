package com.example.solomon.feature.trial.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.solomon.feature.member.domain.entity.Member;

import static org.assertj.core.api.Assertions.assertThat;

public class TrialTest {

    @Test
    @DisplayName("should create a Trial with default values and an initial TrialMember")
    void create() {
        // given, when
        Member member = Member.create(
                "test@test.com",
                "picture.png");

        String issueTitle = "test";
        String nickname = "test";

        Trial trial = Trial.create(member, issueTitle, nickname);

        // then
        assertThat(trial.getIssueTitle()).isEqualTo(issueTitle);
        assertThat(trial.getStage()).isEqualTo(Stage.STAND_BY);
        assertThat(trial.getLastMessage()).isNull();
        assertThat(trial.getLastMessageSeq()).isNull();

        assertThat(trial.getTrialMembers()).hasSize(1);

        TrialMember trialMember = trial.getTrialMembers().get(0);

        assertThat(trialMember.getTrial()).isSameAs(trial);
        assertThat(trialMember.getMember()).isSameAs(member);
        assertThat(trialMember.getNickname()).isEqualTo(nickname);
        assertThat(trialMember.getTurn()).isEqualTo(Turn.STAND_BY);
        assertThat(trialMember.getReadSeq()).isNull();

        assertThat(trial.getTrialMembers()).containsExactly(trialMember);
    }

}
