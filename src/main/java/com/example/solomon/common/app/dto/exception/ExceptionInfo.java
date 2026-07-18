package com.example.solomon.common.app.dto.exception;

import org.springframework.http.HttpStatus;

public interface ExceptionInfo {

    HttpStatus getHttpStatus();

    String getMessage();

}
