package com.magentamause.cosydomainprovider.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "providerSubject"}))
public class OAuthIdentityEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String uuid;

    @ManyToOne(optional = false)
    private UserEntity user;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String providerSubject;

    @Column(nullable = false)
    private String email;
}
