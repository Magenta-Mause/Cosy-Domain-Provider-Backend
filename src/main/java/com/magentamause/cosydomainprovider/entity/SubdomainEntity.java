package com.magentamause.cosydomainprovider.entity;

import com.magentamause.cosydomainprovider.model.core.LabelMode;
import com.magentamause.cosydomainprovider.model.core.SubdomainDto;
import com.magentamause.cosydomainprovider.model.core.SubdomainStatus;
import com.magentamause.cosydomainprovider.model.dns.DnsEntry;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

    @Column(nullable = true)
    private String targetIp;

    @Column(nullable = true)
    private String targetIpv6;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubdomainStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LabelMode labelMode = LabelMode.RANDOM;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    public SubdomainDto toDto(String parentDomain, long defaultTtl) {
        String resolvedFqdn = fqdn != null ? fqdn : label + "." + parentDomain;
        List<DnsEntry> entries = new ArrayList<>();
        if (targetIp != null && !targetIp.isBlank()) {
            entries.add(new DnsEntry(resolvedFqdn, "A", defaultTtl, List.of(targetIp)));
        }
        if (targetIpv6 != null && !targetIpv6.isBlank()) {
            entries.add(new DnsEntry(resolvedFqdn, "AAAA", defaultTtl, List.of(targetIpv6)));
        }
        return SubdomainDto.builder()
                .uuid(uuid)
                .label(label)
                .fqdn(resolvedFqdn)
                .targetIp(targetIp)
                .targetIpv6(targetIpv6)
                .status(status)
                .labelMode(labelMode)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .dnsEntries(entries)
                .build();
    }
}
