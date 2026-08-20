package com.example.solomon.feature.chat.domain.exception;

import com.example.solomon.common.domain.exception.ExceptionInfo;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ChatException implements ExceptionInfo {

    NO_AVAILABLE_SERVER("No chat server is currently available.");

    private final String message;

    @Override
    public String getMessage() {
        return message;
    }

}
