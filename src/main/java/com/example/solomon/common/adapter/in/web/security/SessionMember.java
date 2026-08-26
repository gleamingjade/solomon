package com.example.solomon.common.adapter.in.web.security;

import java.security.Principal;
import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

import javax.security.auth.Subject;

@Getter
public class SessionMember implements Principal {

    private final String id;

    private Collection<? extends GrantedAuthority> authorities;

    private final String httpSessionId;

    // I want this object to remain immutable.
    // But you know that Jackson need to default constructor.
    // So this can be alternative for defining default constructor. @JsonCreator.
    @JsonCreator
    public SessionMember(
            @JsonProperty("id") String id,
            @JsonProperty("authorities") Collection<? extends GrantedAuthority> authorities,
            @JsonProperty("httpSessionId") String httpSessionId) {
        this.id = id;
        this.authorities = authorities;
        this.httpSessionId = httpSessionId;
    }

    @Override
    public String getName() {
        return id;
    }

    @Override
    public boolean implies(Subject subject) {
        return Principal.super.implies(subject);
    }
}