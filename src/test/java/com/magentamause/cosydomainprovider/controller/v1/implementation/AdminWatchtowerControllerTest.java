package com.magentamause.cosydomainprovider.controller.v1.implementation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.magentamause.cosydomainprovider.model.action.WatchtowerReviewUpdateDto;
import com.magentamause.cosydomainprovider.model.action.WatchtowerScanIngestDto;
import com.magentamause.cosydomainprovider.model.admin.AdminWatchtowerScanDto;
import com.magentamause.cosydomainprovider.model.admin.AdminWatchtowerSummaryDto;
import com.magentamause.cosydomainprovider.model.core.WatchtowerCategory;
import com.magentamause.cosydomainprovider.model.core.WatchtowerReviewStatus;
import com.magentamause.cosydomainprovider.services.core.WatchtowerService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AdminWatchtowerControllerTest {

    @Mock private WatchtowerService watchtowerService;

    private AdminWatchtowerController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminWatchtowerController(watchtowerService);
    }

    private static AdminWatchtowerScanDto scan() {
        return AdminWatchtowerScanDto.builder()
                .uuid("s-1")
                .label("swift-gecko")
                .category(WatchtowerCategory.MALICIOUS)
                .reviewStatus(WatchtowerReviewStatus.PENDING)
                .build();
    }

    @Test
    void getSummary_returnsServiceSummary() {
        AdminWatchtowerSummaryDto summary =
                AdminWatchtowerSummaryDto.builder().totalSubdomains(3).flagged(1).build();
        when(watchtowerService.adminGetSummary()).thenReturn(summary);

        ResponseEntity<AdminWatchtowerSummaryDto> response = controller.getSummary("key");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(summary);
    }

    @Test
    void getLatestScans_returnsServiceList() {
        when(watchtowerService.adminGetLatestScans()).thenReturn(List.of(scan()));

        ResponseEntity<List<AdminWatchtowerScanDto>> response = controller.getLatestScans("key");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void getScan_delegatesUuid() {
        when(watchtowerService.adminGetScan("s-1")).thenReturn(scan());

        ResponseEntity<AdminWatchtowerScanDto> response = controller.getScan("key", "s-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(watchtowerService).adminGetScan("s-1");
    }

    @Test
    void getScanHistory_delegatesSubdomainUuid() {
        when(watchtowerService.adminGetScanHistory("sd-1")).thenReturn(List.of(scan()));

        ResponseEntity<List<AdminWatchtowerScanDto>> response =
                controller.getScanHistory("key", "sd-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(watchtowerService).adminGetScanHistory("sd-1");
    }

    @Test
    void ingestScan_returns201Created() {
        WatchtowerScanIngestDto body =
                WatchtowerScanIngestDto.builder().subdomainUuid("sd-1").build();
        when(watchtowerService.ingestScan(body)).thenReturn(scan());

        ResponseEntity<AdminWatchtowerScanDto> response = controller.ingestScan("key", body);

        // The scanner creates a new row every night, so this is a creation, not an
        // update — the 201 is what tells a retrying scanner it actually landed.
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void updateReview_returns200WithUpdatedScan() {
        WatchtowerReviewUpdateDto body =
                WatchtowerReviewUpdateDto.builder()
                        .reviewStatus(WatchtowerReviewStatus.DISMISSED)
                        .build();
        when(watchtowerService.adminUpdateReview("s-1", body)).thenReturn(scan());

        ResponseEntity<AdminWatchtowerScanDto> response =
                controller.updateReview("key", "s-1", body);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(watchtowerService).adminUpdateReview("s-1", body);
    }
}
