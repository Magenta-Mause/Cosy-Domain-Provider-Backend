package com.magentamause.cosydomainprovider.model.admin;

import com.magentamause.cosydomainprovider.model.core.Plan;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminUserDetailDto {
    private final String uuid;
    private final String username;
    private final String email;
    private final boolean isVerified;
    private final Plan tier;
    private final int maxSubdomainCount;
    private final Integer maxSubdomainCountOverride;
    private final Instant planExpiresAt;
    private final Instant createdAt;
    private final List<AdminSubdomainDto> subdomains;
}
