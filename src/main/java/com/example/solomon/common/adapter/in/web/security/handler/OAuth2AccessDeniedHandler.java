package com.example.solomon.common.adapter.in.web.security.handler;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OAuth2AccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        log.error("접근 거부 - 요청 URI: {}, 발생 클래스: {}, 메시지: {}",
                request.getRequestURI(), ExceptionOrigin.classNameOf(accessDeniedException),
                accessDeniedException.getMessage(), accessDeniedException);
    }

}