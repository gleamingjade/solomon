package com.example.solomon.common.adapter.in.web.security.oauth;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public class SessionMember {

    private final String id;

    private Collection<? extends GrantedAuthority> authorities;

    private final String httpSessionId;

    @JsonCreator
    public SessionMember(
            @JsonProperty("id") String id,
            @JsonProperty("authorities") Collection<? extends GrantedAuthority> authorities,
            @JsonProperty("httpSessionId") String httpSessionId) {
        this.id = id;
        this.authorities = authorities;
        this.httpSessionId = httpSessionId;
    }

}