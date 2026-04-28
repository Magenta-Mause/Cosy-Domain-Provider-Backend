package com.magentamause.cosydomainprovider.model.core;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponseDto {
    private String refreshToken;
    private Boolean mfaRequired;
    private String challengeToken;
}
