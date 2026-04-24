package com.magentamause.cosydomainprovider.model.admin;

import com.magentamause.cosydomainprovider.model.core.LabelMode;
import com.magentamause.cosydomainprovider.model.core.SubdomainStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AdminSubdomainDto {
    private final String uuid;
    private final String label;
    private final String fqdn;
    private final String targetIp;
    private final String targetIpv6;
    private final SubdomainStatus status;
    private final LabelMode labelMode;
    private final String ownerUuid;
    private final String ownerUsername;
    private final String ownerEmail;
    private final Instant createdAt;
    private final Instant updatedAt;
}
