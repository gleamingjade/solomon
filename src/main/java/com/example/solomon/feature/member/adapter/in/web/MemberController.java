package com.example.solomon.feature.member.adapter.in.web;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.solomon.feature.member.adapter.in.web.dto.SignUpRequest;
import com.example.solomon.feature.member.application.in.usecase.SignUpUseCase;
import com.example.solomon.feature.member.application.in.usecase.dto.SignUpCommand;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final SignUpUseCase signUpUseCase;

    @PostMapping("/signup")
    public ResponseEntity<Void> signUp(@RequestBody SignUpRequest request) {
        Long memberId = signUpUseCase.execute(new SignUpCommand(request.email(), request.password()));

        return ResponseEntity.created(URI.create("/api/members/" + memberId)).build();
    }

}