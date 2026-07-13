package com.example.solomon.feature.member.application.in.usecase.dto.exception;

import org.springframework.http.HttpStatus;

import com.example.solomon.common.app.dto.exception.ExceptionInfo;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum MemberException implements ExceptionInfo {

    UNEXISTS_MEMBER(HttpStatus.BAD_REQUEST, "unexists member.");

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getMessage() {
        return message;
    }

}