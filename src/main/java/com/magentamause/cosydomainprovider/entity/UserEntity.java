package com.magentamause.cosydomainprovider.entity;

import com.magentamause.cosydomainprovider.model.core.UserDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

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
    private boolean isVerified;
    private boolean needsPasswordSetup;

    public UserDto toDto() {
        return UserDto.builder()
                .uuid(uuid)
                .username(username)
                .email(email)
                .isVerified(isVerified)
                .needsPasswordSetup(needsPasswordSetup)
                .build();
    }
}
