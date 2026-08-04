package com.example.solomon.feature.trial.domain.exception;

import com.example.solomon.common.domain.exception.ExceptionInfo;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TrialException implements ExceptionInfo {

    ONGOING_TRIAL_EXISTS("Ongoing trial exists."),
    CAPACITY_EXCEEDED("More than 2 people are in there"),
    DUPLICATED_NICKNAME("The nickname already has been being used"),
    UNEXISTS_TRIAL("The Trial does not exist");

    private final String message;

    @Override
    public String getMessage() {
        return message;
    }

}
