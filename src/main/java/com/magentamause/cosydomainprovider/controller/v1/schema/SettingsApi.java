package com.magentamause.cosydomainprovider.controller.v1.schema;

import com.magentamause.cosydomainprovider.model.admin.AdminSettingsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Settings", description = "Public application settings")
@RequestMapping("/v1/settings")
public interface SettingsApi {

    @Operation(summary = "Get public application settings")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Settings returned")})
    @GetMapping
    ResponseEntity<AdminSettingsDto> getPublicSettings();
}
