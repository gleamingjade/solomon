package com.example.solomon.common.adapter.in.web.security.form;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.example.solomon.common.adapter.in.web.security.SessionMember;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class FormLoginSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        SessionMember sessionMember = new SessionMember(
                authentication.getName(),
                authentication.getAuthorities(),
                request.getSession(true).getId());

        SessionRedisEmailUser sessionRedisUser = new SessionRedisEmailUser(sessionMember);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        sessionRedisUser, null, sessionRedisUser.getAuthorities()));

        response.setStatus(HttpServletResponse.SC_OK);
    }

}