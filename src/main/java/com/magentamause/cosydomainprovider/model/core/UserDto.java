package com.magentamause.cosydomainprovider.model.core;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDto {
    private final String uuid;
    private final String username;
    private final String email;
    private final boolean isVerified;
}
