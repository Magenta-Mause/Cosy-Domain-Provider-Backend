package com.magentamause.cosydomainprovider.controller.v1.schema;

import com.magentamause.cosydomainprovider.model.action.WatchtowerReviewUpdateDto;
import com.magentamause.cosydomainprovider.model.action.WatchtowerScanIngestDto;
import com.magentamause.cosydomainprovider.model.admin.AdminWatchtowerScanDto;
import com.magentamause.cosydomainprovider.model.admin.AdminWatchtowerSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(
        name = "Admin Watchtower",
        description = "Nightly reputation scans of the sites hosted behind our subdomains")
@RequestMapping("/v1/admin/watchtower")
public interface AdminWatchtowerApi {

    @Operation(summary = "Counters for the Watchtower dashboard header")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Summary returned"),
        @ApiResponse(responseCode = "401", description = "Invalid admin key")
    })
    @GetMapping("/summary")
    ResponseEntity<AdminWatchtowerSummaryDto> getSummary(
            @Parameter(description = "Admin secret key") @RequestHeader("X-Admin-Key")
                    String adminKey);

    @Operation(summary = "Latest scan per subdomain, newest first")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Scan list returned"),
        @ApiResponse(responseCode = "401", description = "Invalid admin key")
    })
    @GetMapping("/scans")
    ResponseEntity<List<AdminWatchtowerScanDto>> getLatestScans(
            @Parameter(description = "Admin secret key") @RequestHeader("X-Admin-Key")
                    String adminKey);

    @Operation(summary = "Get a single scan by UUID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Scan returned"),
        @ApiResponse(responseCode = "401", description = "Invalid admin key"),
        @ApiResponse(responseCode = "404", description = "Scan not found")
    })
    @GetMapping("/scans/{uuid}")
    ResponseEntity<AdminWatchtowerScanDto> getScan(
            @Parameter(description = "Admin secret key") @RequestHeader("X-Admin-Key")
                    String adminKey,
            @Parameter(description = "Scan UUID") @PathVariable String uuid);

    @Operation(summary = "Full scan history for one subdomain, newest first")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "History returned"),
        @ApiResponse(responseCode = "401", description = "Invalid admin key"),
        @ApiResponse(responseCode = "404", description = "Subdomain not found")
    })
    @GetMapping("/subdomains/{subdomainUuid}/scans")
    ResponseEntity<List<AdminWatchtowerScanDto>> getScanHistory(
            @Parameter(description = "Admin secret key") @RequestHeader("X-Admin-Key")
                    String adminKey,
            @Parameter(description = "Subdomain UUID") @PathVariable String subdomainUuid);

    @Operation(summary = "Ingest one verdict from the nightly scanner")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Scan stored"),
        @ApiResponse(responseCode = "400", description = "Invalid request body"),
        @ApiResponse(responseCode = "401", description = "Invalid admin key"),
        @ApiResponse(responseCode = "404", description = "Subdomain not found")
    })
    @PostMapping("/scans")
    ResponseEntity<AdminWatchtowerScanDto> ingestScan(
            @Parameter(description = "Admin secret key") @RequestHeader("X-Admin-Key")
                    String adminKey,
            @Valid @RequestBody WatchtowerScanIngestDto body);

    @Operation(summary = "Record an admin's review decision on a scan")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Review updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request body"),
        @ApiResponse(responseCode = "401", description = "Invalid admin key"),
        @ApiResponse(responseCode = "404", description = "Scan not found")
    })
    @PatchMapping("/scans/{uuid}/review")
    ResponseEntity<AdminWatchtowerScanDto> updateReview(
            @Parameter(description = "Admin secret key") @RequestHeader("X-Admin-Key")
                    String adminKey,
            @Parameter(description = "Scan UUID") @PathVariable String uuid,
            @Valid @RequestBody WatchtowerReviewUpdateDto body);
}
