package com.magentamause.cosydomainprovider.controller.v1.schema;

import com.magentamause.cosydomainprovider.model.action.UpdateUserDto;
import com.magentamause.cosydomainprovider.model.action.UserCreationDto;
import com.magentamause.cosydomainprovider.model.core.OAuthIdentityDto;
import com.magentamause.cosydomainprovider.model.core.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "User", description = "User management (admin)")
@RequestMapping("/v1/user")
public interface UserApi {

    @Operation(summary = "List all users")
    @ApiResponse(responseCode = "200", description = "User list returned")
    @GetMapping
    ResponseEntity<List<UserDto>> getAllUsers();

    @Operation(summary = "Create a new user and send verification email")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User created"),
        @ApiResponse(responseCode = "409", description = "Email already in use")
    })
    @PostMapping
    ResponseEntity<UserDto> createUser(@RequestBody @Valid UserCreationDto userCreationDto);

    @Operation(summary = "Update username or password of the authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User updated"),
        @ApiResponse(responseCode = "401", description = "Current password incorrect")
    })
    @PatchMapping
    ResponseEntity<UserDto> updateUser(@RequestBody @Valid UpdateUserDto dto);

    @Operation(summary = "Delete the authenticated user and all their subdomains")
    @ApiResponse(responseCode = "204", description = "User deleted")
    @DeleteMapping
    ResponseEntity<Void> deleteUser();

    @Operation(summary = "List OAuth identities linked to the authenticated user")
    @ApiResponse(responseCode = "200", description = "Linked identities returned")
    @GetMapping("/oauth-identities")
    ResponseEntity<List<OAuthIdentityDto>> getLinkedIdentities();

    @Operation(summary = "Initiate OAuth account linking for the authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "302", description = "Redirect to provider authorization URL"),
        @ApiResponse(responseCode = "400", description = "Unknown provider")
    })
    @GetMapping("/oauth-identities/{provider}/link")
    ResponseEntity<Void> linkOAuthIdentity(
            @Parameter(description = "OAuth provider name (google, github, discord)") @PathVariable
                    String provider);

    @Operation(summary = "Unlink an OAuth identity from the authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Identity unlinked"),
        @ApiResponse(responseCode = "404", description = "No such identity linked to this account"),
        @ApiResponse(
                responseCode = "409",
                description = "Cannot unlink the only authentication method")
    })
    @DeleteMapping("/oauth-identities/{provider}")
    ResponseEntity<Void> unlinkOAuthIdentity(
            @Parameter(description = "OAuth provider name (google, github, discord)") @PathVariable
                    String provider);
}
