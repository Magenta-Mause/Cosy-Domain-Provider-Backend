package com.magentamause.cosydomainprovider.controller.v1.implementation;

import com.magentamause.cosydomainprovider.controller.v1.schema.AdminWatchtowerApi;
import com.magentamause.cosydomainprovider.model.action.WatchtowerReviewUpdateDto;
import com.magentamause.cosydomainprovider.model.action.WatchtowerScanIngestDto;
import com.magentamause.cosydomainprovider.model.admin.AdminWatchtowerScanDto;
import com.magentamause.cosydomainprovider.model.admin.AdminWatchtowerSummaryDto;
import com.magentamause.cosydomainprovider.services.core.WatchtowerService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminWatchtowerController implements AdminWatchtowerApi {

    private final WatchtowerService watchtowerService;

    @Override
    public ResponseEntity<AdminWatchtowerSummaryDto> getSummary(String adminKey) {
        return ResponseEntity.ok(watchtowerService.adminGetSummary());
    }

    @Override
    public ResponseEntity<List<AdminWatchtowerScanDto>> getLatestScans(String adminKey) {
        return ResponseEntity.ok(watchtowerService.adminGetLatestScans());
    }

    @Override
    public ResponseEntity<AdminWatchtowerScanDto> getScan(String adminKey, String uuid) {
        return ResponseEntity.ok(watchtowerService.adminGetScan(uuid));
    }

    @Override
    public ResponseEntity<List<AdminWatchtowerScanDto>> getScanHistory(
            String adminKey, String subdomainUuid) {
        return ResponseEntity.ok(watchtowerService.adminGetScanHistory(subdomainUuid));
    }

    @Override
    public ResponseEntity<AdminWatchtowerScanDto> ingestScan(
            String adminKey, WatchtowerScanIngestDto body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(watchtowerService.ingestScan(body));
    }

    @Override
    public ResponseEntity<AdminWatchtowerScanDto> updateReview(
            String adminKey, String uuid, WatchtowerReviewUpdateDto body) {
        return ResponseEntity.ok(watchtowerService.adminUpdateReview(uuid, body));
    }
}
