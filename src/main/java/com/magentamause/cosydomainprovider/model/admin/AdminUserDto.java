package com.magentamause.cosydomainprovider.model.admin;

import com.magentamause.cosydomainprovider.model.core.Plan;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AdminUserDto {
    private final String uuid;
    private final String username;
    private final String email;
    private final boolean isVerified;
    private final Plan tier;
    private final int maxSubdomainCount;
    private final Integer maxSubdomainCountOverride;
    private final long subdomainCount;
    private final Instant planExpiresAt;
    private final Instant createdAt;
}
