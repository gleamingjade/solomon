package com.example.solomon.feature.member.domain.exception;

import com.example.solomon.common.domain.entity.exception.ExceptionInfo;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum MemberException implements ExceptionInfo {

    UNEXISTS_MEMBER("Unexists member.");

    private final String message;

    @Override
    public String getMessage() {
        return message;
    }

}
