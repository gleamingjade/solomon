package com.example.solomon.feature.member.adapter.in.web;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.solomon.common.adapter.in.web.dto.ErrorResponse;
import com.example.solomon.common.adapter.in.web.dto.SuccessResponse;
import com.example.solomon.feature.member.adapter.in.web.dto.MemberResponse;
import com.example.solomon.feature.member.adapter.in.web.dto.SignUpRequest;
import com.example.solomon.feature.member.application.in.usecase.GetMemberUseCase;
import com.example.solomon.feature.member.application.in.usecase.SignUpUseCase;
import com.example.solomon.feature.member.application.in.usecase.dto.SignUpCommand;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final SignUpUseCase signUpUseCase;

    private final GetMemberUseCase getMemberUseCase;

    @PostMapping("/signup")
    public ResponseEntity<Void> signUp(@RequestBody SignUpRequest request) {
        Long memberId = signUpUseCase.execute(new SignUpCommand(request.email(), request.password()));

        return ResponseEntity.created(URI.create("/api/member/" + memberId)).build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.of(HttpStatus.UNAUTHORIZED, "Authentication required."));
        }

        Long memberId = Long.valueOf(authentication.getName());

        return ResponseEntity.ok(SuccessResponse.of(MemberResponse.from(getMemberUseCase.execute(memberId))));
    }

}