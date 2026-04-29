package com.magentamause.cosydomainprovider.model.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminSettingsDto {
    private final boolean domainCreationEnabled;
}
