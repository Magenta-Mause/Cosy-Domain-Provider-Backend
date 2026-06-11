package com.magentamause.cosydomainprovider.model.action;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminMaxSubdomainOverrideDto {
    /** Null clears the override and falls back to the plan default. */
    @Min(0)
    private Integer maxSubdomainCountOverride;
}
