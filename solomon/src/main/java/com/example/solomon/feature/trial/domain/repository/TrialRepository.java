package com.example.solomon.feature.trial.domain.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.solomon.feature.trial.domain.entity.Stage;
import com.example.solomon.feature.trial.domain.entity.Trial;

public interface TrialRepository extends JpaRepository<Trial, UUID> {

    @Query("""
                select count(distinct tr)
                from Trial tr
                join tr.trialMembers tm
                where tm.member.id = :memberId
                  and tr.stage <> :stage
            """)
    long countUnTerminatedTrialByMemberId(
            @Param("memberId") Long memberId,
            @Param("stage") Stage stage);

}