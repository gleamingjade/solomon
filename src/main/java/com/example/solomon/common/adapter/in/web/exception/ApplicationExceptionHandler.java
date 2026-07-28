package com.example.solomon.common.adapter.in.web.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.solomon.common.domain.entity.exception.ApplicationException;

import lombok.RequiredArgsConstructor;

@RestControllerAdvice
@RequiredArgsConstructor
public class ApplicationExceptionHandler {

    private final ExceptionInfoHttpStatusResolver exceptionInfoHttpStatusResolver;

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<String> handleApplicationException(ApplicationException exception) {
        HttpStatus httpStatus = exceptionInfoHttpStatusResolver.resolve(exception.getExceptionInfo());

        return ResponseEntity.status(httpStatus).body(exception.getExceptionInfo().getMessage());
    }

}
