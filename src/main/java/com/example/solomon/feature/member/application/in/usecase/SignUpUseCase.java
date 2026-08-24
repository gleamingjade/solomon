package com.example.solomon.feature.member.application.in.usecase;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.solomon.common.domain.exception.BusinessException;
import com.example.solomon.feature.member.application.in.usecase.dto.SignUpCommand;
import com.example.solomon.feature.member.application.out.MemberRepository;
import com.example.solomon.feature.member.domain.entity.Member;
import com.example.solomon.feature.member.domain.exception.MemberException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SignUpUseCase {

    private final MemberRepository memberRepository;

    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long execute(SignUpCommand command) {
        memberRepository.findByEmail(command.email())
                .ifPresent(member -> {
                    throw new BusinessException(MemberException.DUPLICATE_EMAIL);
                });

        Member member = memberRepository.save(
                Member.createByEmail(command.email(), passwordEncoder.encode(command.password())));

        return member.getId();
    }

}

/// 모놀리스라지만  ExceptionInfoHttpStatusResolver에 DUPLICATE_EMAIL → 400 BAD_REQUEST로 매핑도 돼 있음
// 이거 허용할건지.. 그리고 사인인 제대로 안 되는 거 .. 고치고 전체 검토하고 문서까지..