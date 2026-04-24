package com.magentamause.cosydomainprovider.model.core;

import com.magentamause.cosydomainprovider.model.dns.DnsEntry;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubdomainDto {
    private final String uuid;
    private final String label;
    private final String fqdn;
    private final String targetIp;
    private final String targetIpv6;
    private final SubdomainStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final List<DnsEntry> dnsEntries;
}
