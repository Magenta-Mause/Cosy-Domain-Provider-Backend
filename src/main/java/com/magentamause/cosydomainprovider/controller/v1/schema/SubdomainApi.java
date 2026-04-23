package com.magentamause.cosydomainprovider.controller.v1.schema;

import com.magentamause.cosydomainprovider.model.action.SubdomainCreationDto;
import com.magentamause.cosydomainprovider.model.action.SubdomainUpdateDto;
import com.magentamause.cosydomainprovider.model.core.LabelAvailabilityDto;
import com.magentamause.cosydomainprovider.model.core.SubdomainDto;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(
        name = "Subdomain",
        description = "Subdomain creation and management for the authenticated user")
@RequestMapping("/v1/subdomain")
public interface SubdomainApi {

    @Operation(summary = "List all subdomains owned by the authenticated user")
    @ApiResponse(responseCode = "200", description = "Subdomain list returned")
    @GetMapping
    ResponseEntity<List<SubdomainDto>> listMySubdomains();

    @Operation(summary = "Check whether a subdomain label is available")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Availability result returned"),
        @ApiResponse(responseCode = "400", description = "Label is reserved or invalid")
    })
    @GetMapping("/check")
    ResponseEntity<LabelAvailabilityDto> checkLabelAvailability(
            @Parameter(description = "Subdomain label to check (without parent domain)")
                    @RequestParam
                    String label);

    @Operation(summary = "Get a specific subdomain owned by the authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Subdomain returned"),
        @ApiResponse(responseCode = "403", description = "Not owned by the current user"),
        @ApiResponse(responseCode = "404", description = "Subdomain not found")
    })
    @GetMapping("/{uuid}")
    ResponseEntity<SubdomainDto> getSubdomain(
            @Parameter(description = "Subdomain UUID") @PathVariable String uuid);

    @Operation(summary = "Create a new subdomain")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Subdomain created"),
        @ApiResponse(responseCode = "400", description = "Label reserved or invalid"),
        @ApiResponse(responseCode = "409", description = "Label already taken"),
        @ApiResponse(responseCode = "429", description = "Subdomain limit reached for this plan")
    })
    @PostMapping
    ResponseEntity<SubdomainDto> createSubdomain(
            @Valid @RequestBody SubdomainCreationDto creationDto);

    @Operation(summary = "Update the target IP of a subdomain")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Subdomain updated"),
        @ApiResponse(responseCode = "403", description = "Not owned by the current user"),
        @ApiResponse(responseCode = "404", description = "Subdomain not found")
    })
    @PutMapping("/{uuid}")
    ResponseEntity<SubdomainDto> updateSubdomain(
            @Parameter(description = "Subdomain UUID") @PathVariable String uuid,
            @Valid @RequestBody SubdomainUpdateDto updateDto);

    @Operation(summary = "Delete a subdomain and its DNS record")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Subdomain deleted"),
        @ApiResponse(responseCode = "403", description = "Not owned by the current user"),
        @ApiResponse(responseCode = "404", description = "Subdomain not found")
    })
    @DeleteMapping("/{uuid}")
    ResponseEntity<Void> deleteSubdomain(
            @Parameter(description = "Subdomain UUID") @PathVariable String uuid);
}
