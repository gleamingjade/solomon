package com.example.solomon.common.domain.entity.exception;

import org.springframework.http.HttpStatus;

public interface ExceptionInfo {

    HttpStatus getHttpStatus();

    String getMessage();

}
