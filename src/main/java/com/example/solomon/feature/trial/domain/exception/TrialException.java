package com.example.solomon.feature.trial.domain.exception;

import com.example.solomon.common.domain.entity.exception.ExceptionInfo;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TrialException implements ExceptionInfo {

    ONGOING_TRIAL_EXISTS("Ongoing trial exists."),
    UNEXISTS_TRIAL("ongoing trial exists.");

    private final String message;

    @Override
    public String getMessage() {
        return message;
    }

}
