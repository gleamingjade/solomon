package com.example.solomon.feature.member.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

import com.example.solomon.common.infra.persistence.jpa.domain.entity.IdBaseEntity;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Member extends IdBaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String picture;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role;

    @Column(nullable = false)
    private int balance;

    @Column(nullable = false)
    private LocalDateTime lastFreeAwardedAt;

    @Column(nullable = false)
    private LocalDateTime lastAdAwardedAt;

    public static Member create(String email, String picture) {
        Member m = new Member();

        m.email = email;
        m.picture = picture;
        m.role = MemberRole.FREE;

        m.balance = 1;
        m.lastFreeAwardedAt = LocalDateTime.now();
        m.lastAdAwardedAt = LocalDateTime.of(1970, 1, 1, 0, 0, 0);

        return m;
    }

}