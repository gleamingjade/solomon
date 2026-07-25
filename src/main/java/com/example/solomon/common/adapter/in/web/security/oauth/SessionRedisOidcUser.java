package com.example.solomon.common.adapter.in.web.security.oauth;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.Map;

@JsonAutoDetect(
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class SessionRedisOidcUser implements OidcUser {

    @JsonProperty("sessionMember")
    private final SessionMember sessionMember;

    @JsonCreator
    public SessionRedisOidcUser(@JsonProperty("sessionMember") SessionMember sessionMember) {
        this.sessionMember = sessionMember;
    }

    @Override
    public Map<String, Object> getClaims() {
        throw new UnsupportedOperationException("claims are not retained in the session");
    }

    @Override
    public OidcUserInfo getUserInfo() {
        throw new UnsupportedOperationException("user info is not retained in the session");
    }

    @Override
    public OidcIdToken getIdToken() {
        throw new UnsupportedOperationException("ID token is not retained in the session");
    }

    @Override
    public Map<String, Object> getAttributes() {
        throw new UnsupportedOperationException("attributes are not retained in the session");
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
