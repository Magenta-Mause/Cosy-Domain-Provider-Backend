package com.magentamause.cosydomainprovider.model.action;

import com.magentamause.cosydomainprovider.model.core.WatchtowerCategory;
import com.magentamause.cosydomainprovider.model.core.WatchtowerRiskLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One verdict posted back by the nightly Watchtower scanner. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WatchtowerScanIngestDto {

    @NotBlank private String subdomainUuid;

    @NotNull private Instant scannedAt;

    @NotNull private Boolean reachable;

    private Integer httpStatus;

    @NotNull private WatchtowerCategory category;

    @NotNull private WatchtowerRiskLevel riskLevel;

    @NotBlank
    @Size(max = 2000)
    private String summary;

    @Builder.Default private List<String> visitedPaths = List.of();

    /** MinIO object key the scanner already uploaded the screenshot under. */
    private String screenshotKey;

    @NotBlank private String modelId;
}
