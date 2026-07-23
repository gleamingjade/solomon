package com.example.solomon.common.adapter.in.web.security.oauth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.Map;

public class SessionRedisOidcUser implements OidcUser {

    private final DefaultOidcUser delegate;

    private final SessionMember sessionMember;

    @JsonCreator
    public SessionRedisOidcUser(
            @JsonProperty("delegate") DefaultOidcUser delegate,
            @JsonProperty("sessionMember") SessionMember sessionMember) {
        this.delegate = delegate;
        this.sessionMember = sessionMember;
    }

    public SessionMember getSessionMember() {
        return sessionMember;
    }

    @Override
    public Map<String, Object> getClaims() {
        return delegate.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return delegate.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return delegate.getIdToken();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return sessionMember.getAuthorities();
    }

    @Override
    public String getName() {
        return sessionMember.getId();
    }

}