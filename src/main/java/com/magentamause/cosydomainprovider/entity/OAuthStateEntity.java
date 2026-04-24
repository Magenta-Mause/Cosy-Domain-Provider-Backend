package com.magentamause.cosydomainprovider.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class OAuthStateEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private String state;

    @Column(nullable = false, updatable = false)
    private Instant issuedAt;
}
