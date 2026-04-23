package com.magentamause.cosydomainprovider.entity;

import com.magentamause.cosydomainprovider.model.core.Plan;
import com.magentamause.cosydomainprovider.model.core.UserDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String uuid;

    @Column(nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;
    private String passwordHash;

    private String accessToken;
    private Instant accessTokenExpiresAt;
    private boolean isVerified;
    private boolean needsPasswordSetup;

    private String passwordResetToken;
    private Instant passwordResetExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Plan plan = Plan.FREE;

    private Instant planExpiresAt;
    private String stripeCustomerId;

    public UserDto toDto() {
        return UserDto.builder()
                .uuid(uuid)
                .username(username)
                .email(email)
                .isVerified(isVerified)
                .needsPasswordSetup(needsPasswordSetup)
                .plan(plan)
                .build();
    }
}
