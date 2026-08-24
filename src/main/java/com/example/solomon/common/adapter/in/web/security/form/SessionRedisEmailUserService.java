package com.example.solomon.common.adapter.in.web.security.form;

import java.util.List;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.solomon.feature.member.application.out.MemberRepository;
import com.example.solomon.feature.member.domain.entity.Member;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SessionRedisEmailUserService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email));

        if (member.getPassword() == null) {
            throw new UsernameNotFoundException(email);
        }

        return new User(
                String.valueOf(member.getId()),
                member.getPassword(),
                List.of(member.getRole().toGrantedAuthority()));
    }

}