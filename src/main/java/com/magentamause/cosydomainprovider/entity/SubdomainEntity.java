package com.magentamause.cosydomainprovider.entity;

import com.magentamause.cosydomainprovider.model.core.SubdomainDto;
import com.magentamause.cosydomainprovider.model.core.SubdomainStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class SubdomainEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String uuid;

    @Column(unique = true, nullable = false)
    private String label;

    @Column(nullable = true)
    private String fqdn;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_uuid", nullable = false)
    private UserEntity owner;

    @Column(nullable = false)
    private String targetIp;

    @Column(nullable = true)
    private String targetIpv6;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubdomainStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public SubdomainDto toDto(String parentDomain) {
        return SubdomainDto.builder()
                .uuid(uuid)
                .label(label)
                .fqdn(fqdn != null ? fqdn : label + "." + parentDomain)
                .targetIp(targetIp)
                .targetIpv6(targetIpv6)
                .status(status)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
