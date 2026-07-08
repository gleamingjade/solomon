package com.example.solomon.feature.trial.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.solomon.feature.trial.domain.entity.TrialMember;
import com.example.solomon.feature.trial.domain.entity.TrialMemberId;

public interface TrialMemberRepository extends JpaRepository<TrialMember, TrialMemberId> {

    public List<TrialMember> findAllByTrialId(UUID trialId);

}