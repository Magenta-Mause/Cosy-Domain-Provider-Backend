package com.magentamause.cosydomainprovider.model.action;

import com.magentamause.cosydomainprovider.model.core.WatchtowerReviewStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** An admin's decision on a scan the agent flagged. */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WatchtowerReviewUpdateDto {

    @NotNull private WatchtowerReviewStatus reviewStatus;

    @Size(max = 2000)
    private String reviewNote;
}
