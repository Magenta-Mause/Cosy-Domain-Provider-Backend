package com.magentamause.cosydomainprovider.model.action;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MfaChallengeDto {
    @NotBlank private String challengeToken;

    @NotBlank
    @Size(min = 6, max = 6)
    private String totpCode;
}
