package com.magentamause.cosydomainprovider.controller.v1.implementation;

import com.magentamause.cosydomainprovider.controller.v1.schema.SettingsApi;
import com.magentamause.cosydomainprovider.model.admin.AdminSettingsDto;
import com.magentamause.cosydomainprovider.services.core.GlobalSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SettingsController implements SettingsApi {

    private final GlobalSettingsService globalSettingsService;

    @Override
    public ResponseEntity<AdminSettingsDto> getPublicSettings() {
        return ResponseEntity.ok(
                AdminSettingsDto.builder()
                        .domainCreationEnabled(globalSettingsService.isDomainCreationEnabled())
                        .build());
    }
}
