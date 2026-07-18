package com.example.solomon.common.app.dto.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AppException extends RuntimeException {

    private final ExceptionInfo exceptionInfo;

}