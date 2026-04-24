package com.magentamause.cosydomainprovider.controller.v1.schema;

import com.magentamause.cosydomainprovider.model.admin.AdminSubdomainDto;
import com.magentamause.cosydomainprovider.model.admin.AdminUserDetailDto;
import com.magentamause.cosydomainprovider.model.admin.AdminUserDto;
import com.magentamause.cosydomainprovider.model.core.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Admin", description = "Admin-only endpoints protected by a shared secret key")
@RequestMapping("/v1/admin")
public interface AdminApi {

    @Operation(summary = "List all subdomains")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Subdomain list returned"),
        @ApiResponse(responseCode = "401", description = "Invalid admin key")
    })
    @GetMapping("/subdomains")
    ResponseEntity<List<AdminSubdomainDto>> getAllSubdomains(
            @Parameter(description = "Admin secret key") @RequestHeader("X-Admin-Key")
                    String adminKey);

    @Operation(summary = "List all users")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User list returned"),
        @ApiResponse(responseCode = "401", description = "Invalid admin key")
    })
    @GetMapping("/users")
    ResponseEntity<List<AdminUserDto>> getAllUsers(
            @Parameter(description = "Admin secret key") @RequestHeader("X-Admin-Key")
                    String adminKey);

    @Operation(summary = "Get user detail including their subdomains")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User detail returned"),
        @ApiResponse(responseCode = "401", description = "Invalid admin key"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/users/{uuid}")
    ResponseEntity<AdminUserDetailDto> getUserDetail(
            @Parameter(description = "Admin secret key") @RequestHeader("X-Admin-Key")
                    String adminKey,
            @Parameter(description = "User UUID") @PathVariable String uuid);

    @Operation(summary = "Override the maximum subdomain count for a user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Override updated"),
        @ApiResponse(responseCode = "401", description = "Invalid admin key"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/users/{uuid}/max-subdomain-override")
    ResponseEntity<UserDto> setMaxSubdomainOverride(
            @Parameter(description = "Admin secret key") @RequestHeader("X-Admin-Key")
                    String adminKey,
            @Parameter(description = "User UUID") @PathVariable String uuid,
            @RequestBody Map<String, Integer> body);
}
