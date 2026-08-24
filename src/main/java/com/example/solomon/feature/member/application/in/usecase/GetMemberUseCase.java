package com.example.solomon.feature.member.application.in.usecase;

import org.springframework.stereotype.Service;

import com.example.solomon.common.domain.exception.BusinessException;
import com.example.solomon.feature.member.application.out.MemberRepository;
import com.example.solomon.feature.member.domain.entity.Member;
import com.example.solomon.feature.member.domain.exception.MemberException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetMemberUseCase {

    private final MemberRepository memberRepository;

    public Member execute(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(MemberException.UNEXISTS_MEMBER));
    }

}