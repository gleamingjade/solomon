package com.example.solomon.feature.member.adapter.in.web.dto;

import com.example.solomon.feature.member.domain.entity.Member;
import com.example.solomon.feature.member.domain.entity.MemberRole;

public record MemberResponse(Long id, String email, String picture, MemberRole role, Integer balance) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getPicture(),
                member.getRole(),
                member.getBalance());
    }

}