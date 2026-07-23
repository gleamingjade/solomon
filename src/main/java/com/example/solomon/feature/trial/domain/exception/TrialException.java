package com.example.solomon.feature.trial.domain.exception;

import org.springframework.http.HttpStatus;

import com.example.solomon.common.domain.entity.exception.ExceptionInfo;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TrialException implements ExceptionInfo {

    ONGOING_TRIAL_EXISTS(HttpStatus.BAD_REQUEST, "ongoing trial exists."),
    UNEXISTS_TRIAL(HttpStatus.BAD_REQUEST, "ongoing trial exists.");

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