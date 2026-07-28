package com.example.solomon.common.domain.entity.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ApplicationException extends RuntimeException {

    private final ExceptionInfo exceptionInfo;

}
