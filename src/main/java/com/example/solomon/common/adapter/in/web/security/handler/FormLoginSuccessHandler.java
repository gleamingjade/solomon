package com.example.solomon.common.adapter.in.web.security.handler;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class FormLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final String frontEndOrigin;

    public FormLoginSuccessHandler(@Value("${FRONT_END_ORIGIN}") String frontEndOrigin) {
        this.frontEndOrigin = frontEndOrigin;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        log.debug("이메일 로그인 성공");

        response.sendRedirect(frontEndOrigin + "/");
    }

}