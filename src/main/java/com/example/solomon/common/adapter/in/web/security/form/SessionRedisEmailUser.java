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
public record SessionRedisEmailUser(
        @JsonProperty("sessionMember") SessionMember sessionMember) implements UserDetails, SessionMemberHolder {

    @JsonCreator
    public SessionRedisEmailUser {
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return sessionMember.getAuthorities();
    }

    @Override
    public String getPassword() {
        throw new UnsupportedOperationException("Password is not retained in the session.");
    }

    @Override
    public String getUsername() {
        return sessionMember.getId();
    }

    @Override
    public SessionMember sessionMember() {
        return sessionMember;
    }

}