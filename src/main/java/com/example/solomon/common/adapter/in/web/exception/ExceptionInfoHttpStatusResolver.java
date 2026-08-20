package com.example.solomon.common.adapter.in.web.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.example.solomon.common.domain.exception.ExceptionInfo;
import com.example.solomon.feature.chat.domain.exception.ChatException;
import com.example.solomon.feature.member.domain.exception.MemberException;
import com.example.solomon.feature.trial.domain.exception.TrialException;

@Component
public class ExceptionInfoHttpStatusResolver {

    private static final Map<ExceptionInfo, HttpStatus> STATUS_BY_EXCEPTION_INFO = Map.of(
            TrialException.ONGOING_TRIAL_EXISTS, HttpStatus.BAD_REQUEST,
            TrialException.UNEXISTS_TRIAL, HttpStatus.NOT_FOUND,
            TrialException.CAPACITY_EXCEEDED, HttpStatus.BAD_REQUEST,
            TrialException.DUPLICATED_NICKNAME, HttpStatus.BAD_REQUEST,
            MemberException.UNEXISTS_MEMBER, HttpStatus.NOT_FOUND,
            MemberException.DUPLICATE_EMAIL, HttpStatus.BAD_REQUEST,
            ChatException.NO_AVAILABLE_SERVER, HttpStatus.SERVICE_UNAVAILABLE);

    public HttpStatus resolve(ExceptionInfo exceptionInfo) {
        return STATUS_BY_EXCEPTION_INFO.getOrDefault(exceptionInfo, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}

