package com.example.solomon.common.adapter.in.web.dto;

public record SuccessResponse<T>(T data) {

    public static <T> SuccessResponse<T> of(T data) {
        return new SuccessResponse<>(data);
    }

}