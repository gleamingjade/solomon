package com.example.solomon.feature.trial.adapter.out.persistence.jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.example.solomon.feature.trial.domain.entity.Trial;

public interface SpringDataJpaTrialRepository extends JpaRepository<Trial, UUID> {

        // Conditional on the incoming sequence being newer so an out-of-order retry (see the
        // "Chat Persistence: Kafka Streams State Store" wiki doc) can never regress the preview
        // to stale content - a plain load-then-save from application code can't make this check
        // atomic against a concurrent write, so this has to be a single UPDATE statement.
        @Modifying
        @Transactional
        @Query("""
                            update Trial t
                            set t.lastMessage = :lastMessage, t.lastMessageSeq = :lastMessageSeq
                            where t.id = :id
                              and (t.lastMessageSeq is null or t.lastMessageSeq < :lastMessageSeq)
                        """)
        int updateLastMessageIfNewer(@Param("id") UUID id, @Param("lastMessage") String lastMessage,
                        @Param("lastMessageSeq") Long lastMessageSeq);

        @Query("""
                            select count(distinct tr)
                            from Trial tr
                            join tr.trialMembers tm
                            where tm.member.id = :memberId
                              and tr.stage <> 'TERMINATED'
                        """)
        long countUnTerminatedTrialByMemberId(@Param("memberId") Long memberId);

        @Query("""
                            select distinct t
                            from Trial t
                            left join fetch t.trialMembers
                            where t.id = :id
                        """)
        Optional<Trial> findByIdWithTrialMembers(@Param("id") UUID id);

}