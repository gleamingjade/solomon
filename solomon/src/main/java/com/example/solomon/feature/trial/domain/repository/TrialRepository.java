package com.example.solomon.feature.trial.domain.repository;

import java.util.Optional;
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

        @Query("""
                            select distinct t
                            from Trial t
                            left join fetch t.trialMembers
                            where t.id = :id
                        """)
        Optional<Trial> findByIdWithTrialMembers(@Param("id") UUID id);

}