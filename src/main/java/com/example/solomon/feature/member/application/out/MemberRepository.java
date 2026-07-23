package com.example.solomon.feature.member.application.out;

import com.example.solomon.feature.member.domain.entity.Member;

import java.util.Optional;

public interface MemberRepository {

    Member save(Member member);

    Optional<Member> findById(Long id);

    Optional<Member> findByEmail(String email);

}
