package com.example.solomon.feature.member.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.solomon.feature.member.domain.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

}
