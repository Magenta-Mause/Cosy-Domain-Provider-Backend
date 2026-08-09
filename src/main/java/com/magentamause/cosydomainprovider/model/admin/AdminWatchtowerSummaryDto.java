package com.magentamause.cosydomainprovider.model.admin;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

/** The counters across the top of the Watchtower dashboard. */
@Data
@Builder
public class AdminWatchtowerSummaryDto {
    private final long totalSubdomains;
    private final long scannedSubdomains;
    private final long cosyFrontends;
    private final long benign;
    private final long flagged;
    private final long unreachable;
    private final long pendingReview;
    private final Instant lastScanAt;
}
