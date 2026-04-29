package com.magentamause.cosydomainprovider.controller.v1.schema;

import com.magentamause.cosydomainprovider.model.action.AdminSubdomainRelabelDto;
import com.magentamause.cosydomainprovider.model.action.AdminUserUpdateDto;
import com.magentamause.cosydomainprovider.model.action.SubdomainUpdateDto;
import com.magentamause.cosydomainprovider.model.admin.AdminSettingsDto;
import com.magentamause.cosydomainprovider.model.admin.AdminSubdomainDto;
import com.magentamause.cosydomainprovider.model.admin.AdminUserDetailDto;
import com.magentamause.cosydomainprovider.model.admin.AdminUserDto;
import com.magentamause.cosydomainprovider.model.core.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
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

    @Operation(summary = "Get a single subdomain by UUID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Subdomain returned"),
        @ApiResponse(responseCode = "401", description = "Invalid admin key"),
        @ApiResponse(responseCode = "404", description = "Subdomain not found")
    })
    @GetMapping("/subdomains/{uuid}")
    ResponseEntity<AdminSubdomainDto> getSubdomain(
            @Parameter(description = "Admin secret key") @RequestHeader("X-Admin-Key")
                    String adminKey,
            @Parameter(description = "Subdomain UUID") @PathVariable String uuid);

    @Operation(summary = "Update the target IP addresses of a subdomain")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Subdomain updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request body"),
        @ApiResponse(responseCode = "401", description = "Invalid admin key"),
        @ApiResponse(responseCode = "404", description = "Subdomain not found")
    })
    @PutMapping("/subdomains/{uuid}")
    ResponseEntity<AdminSubdomainDto> updateSubdomainTargetIp(
            @Parameter(description = "Admin secret key") @RequestHeader("X-Admin-Key")
                    String adminKey,
            @Parameter(description = "Subdomain UUID") @PathVariable String uuid,
            @Valid @RequestBody SubdomainUpdateDto body);

    @Operation(summary = "Rename a subdomain label")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Subdomain relabeled"),
        @ApiResponse(responseCode = "400", description = "Invalid request body"),
        @ApiResponse(responseCode = "401", description = "Invalid admin key"),
        @ApiResponse(responseCode = "404", description = "Subdomain not found"),
        @ApiResponse(responseCode = "409", description = "Label reserved or already taken")
    })
    @PatchMapping("/subdomains/{uuid}/label")
    ResponseEntity<AdminSubdomainDto> relabelSubdomain(
            @Parameter(description = "Admin secret key") @RequestHeader("X-Admin-Key")
                    String adminKey,
            @Parameter(description = "Subdomain UUID") @PathVariable String uuid,
            @Valid @RequestBody AdminSubdomainRelabelDto body);

    @Operation(summary = "Delete a subdomain")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Subdomain deleted"),
        @ApiResponse(responseCode = "401", description = "Invalid admin key"),
        @ApiResponse(responseCode = "404", description = "Subdomain not found")
    })
    @DeleteMapping("/subdomains/{uuid}")
    ResponseEntity<Void> deleteSubdomain(
            @Parameter(description = "Admin secret key") @RequestHeader("X-Admin-Key")
                    String adminKey,
            @Parameter(description = "Subdomain UUID") @PathVariable String uuid);

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

    @Operation(summary = "Update a user's username and/or email")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request body"),
        @ApiResponse(responseCode = "401", description = "Invalid admin key"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "409", description = "Email already in use")
    })
    @PatchMapping("/users/{uuid}")
    ResponseEntity<AdminUserDto> updateUser(
            @Parameter(description = "Admin secret key") @RequestHeader("X-Admin-Key")
                    String adminKey,
            @Parameter(description = "User UUID") @PathVariable String uuid,
            @Valid @RequestBody AdminUserUpdateDto body);

    @Operation(summary = "Delete a user and all their subdomains")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "User deleted"),
        @ApiResponse(responseCode = "401", description = "Invalid admin key"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/users/{uuid}")
    ResponseEntity<Void> deleteUser(
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

    @Operation(summary = "Get global admin settings")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Settings returned"),
        @ApiResponse(responseCode = "401", description = "Invalid admin key")
    })
    @GetMapping("/settings")
    ResponseEntity<AdminSettingsDto> getSettings(
            @Parameter(description = "Admin secret key") @RequestHeader("X-Admin-Key")
                    String adminKey);

    @Operation(summary = "Update global admin settings")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Settings updated"),
        @ApiResponse(responseCode = "401", description = "Invalid admin key")
    })
    @PatchMapping("/settings")
    ResponseEntity<AdminSettingsDto> updateSettings(
            @Parameter(description = "Admin secret key") @RequestHeader("X-Admin-Key")
                    String adminKey,
            @RequestBody Map<String, Boolean> body);
}
