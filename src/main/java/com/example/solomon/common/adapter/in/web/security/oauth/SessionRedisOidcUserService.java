package com.example.solomon.common.adapter.in.web.security.oauth;

import com.example.solomon.common.adapter.in.web.security.SessionMember;
import com.example.solomon.feature.member.application.out.MemberRepository;
import com.example.solomon.feature.member.domain.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionRedisOidcUserService extends OidcUserService {

    private final MemberRepository memberRepository;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        DefaultOidcUser oidcUser = (DefaultOidcUser) super.loadUser(userRequest);

        Member member = memberRepository.findByEmail(oidcUser.getEmail())
                .orElseGet(() -> memberRepository.save(Member.create(oidcUser.getEmail(), oidcUser.getPicture())));

        SessionMember sessionMember = new SessionMember(
                String.valueOf(member.getId()),
                new ArrayList<>(List.of(member.getRole().toGrantedAuthority())),
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getSession(true).getId());

        return new SessionRedisOidcUser(sessionMember);
    }

}