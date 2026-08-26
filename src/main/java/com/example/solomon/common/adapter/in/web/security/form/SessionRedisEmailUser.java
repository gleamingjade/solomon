package com.example.solomon.common.adapter.in.web.security.form;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.solomon.common.adapter.in.web.security.SessionMember;
import com.example.solomon.common.adapter.in.web.security.SessionMemberHolder;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class SessionRedisEmailUser implements UserDetails, SessionMemberHolder {

    @JsonProperty("sessionMember")
    private final SessionMember sessionMember;

    @JsonCreator
    public SessionRedisEmailUser(@JsonProperty("sessionMember") SessionMember sessionMember) {
        this.sessionMember = sessionMember;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return sessionMember.getAuthorities();
    }

    @Override
    public String getPassword() {
        throw new UnsupportedOperationException("password is not retained in the session");
    }

    @Override
    public String getUsername() {
        return sessionMember.getId();
    }

    @Override
    public SessionMember getSessionMember() {
        return sessionMember;
    }

}