package com.magentamause.cosydomainprovider.controller.v1.schema;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "OAuth", description = "OAuth2 social login (Google, GitHub, Discord)")
@RequestMapping("/api/v1/auth/oauth")
public interface OAuthApi {

    @Operation(summary = "Initiate OAuth2 authorization flow",
            description = "Redirects the user to the provider's authorization page. Supported providers: google, github, discord.")
    @ApiResponses({
        @ApiResponse(responseCode = "302", description = "Redirect to provider authorization URL"),
        @ApiResponse(responseCode = "400", description = "Unknown provider")
    })
    @GetMapping("/{provider}/authorize")
    void authorize(
            @Parameter(description = "OAuth provider name (google, github, discord)", required = true)
            @PathVariable String provider,
            HttpServletResponse response) throws IOException;

    @Operation(summary = "Handle OAuth2 callback",
            description = "Exchanges the authorization code for a token, resolves or creates the local user, and sets the refresh token cookie. Redirects to the frontend on success or failure.")
    @ApiResponses({
        @ApiResponse(responseCode = "302", description = "Redirect to frontend (dashboard on success, /login?oauthError=true on failure)"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired state / unknown provider"),
        @ApiResponse(responseCode = "502", description = "Token exchange or user-info fetch failed")
    })
    @GetMapping("/{provider}/callback")
    void callback(
            @Parameter(description = "OAuth provider name (google, github, discord)", required = true)
            @PathVariable String provider,
            @Parameter(description = "Authorization code returned by the provider", required = true)
            @RequestParam String code,
            @Parameter(description = "State parameter for CSRF validation", required = true)
            @RequestParam String state,
            HttpServletResponse response) throws IOException;
}
