package com.magentamause.cosydomainprovider.model.admin;

import com.magentamause.cosydomainprovider.model.core.WatchtowerCategory;
import com.magentamause.cosydomainprovider.model.core.WatchtowerReviewStatus;
import com.magentamause.cosydomainprovider.model.core.WatchtowerRiskLevel;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminWatchtowerScanDto {
    private final String uuid;
    private final String subdomainUuid;
    private final String label;
    private final String fqdn;
    private final String ownerUuid;
    private final String ownerUsername;
    private final Instant scannedAt;
    private final boolean reachable;
    private final Integer httpStatus;
    private final WatchtowerCategory category;
    private final WatchtowerRiskLevel riskLevel;
    private final String summary;
    private final List<String> visitedPaths;

    /**
     * Short-lived presigned MinIO URL for the screenshot, or null when there is no screenshot. Not
     * persisted — regenerated on every read.
     */
    private final String screenshotUrl;

    private final String modelId;
    private final WatchtowerReviewStatus reviewStatus;
    private final String reviewNote;
    private final Instant reviewedAt;
}
