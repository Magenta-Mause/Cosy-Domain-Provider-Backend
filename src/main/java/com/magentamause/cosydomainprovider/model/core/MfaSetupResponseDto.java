package com.magentamause.cosydomainprovider.model.core;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MfaSetupResponseDto {
    private String totpUri;
    private String secret;
}
