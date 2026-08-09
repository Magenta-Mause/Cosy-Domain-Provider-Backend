package com.magentamause.cosydomainprovider.services.core;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.magentamause.cosydomainprovider.entity.SubdomainEntity;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.entity.WatchtowerScanEntity;
import com.magentamause.cosydomainprovider.model.action.WatchtowerReviewUpdateDto;
import com.magentamause.cosydomainprovider.model.action.WatchtowerScanIngestDto;
import com.magentamause.cosydomainprovider.model.admin.AdminWatchtowerScanDto;
import com.magentamause.cosydomainprovider.model.admin.AdminWatchtowerSummaryDto;
import com.magentamause.cosydomainprovider.model.core.WatchtowerCategory;
import com.magentamause.cosydomainprovider.model.core.WatchtowerReviewStatus;
import com.magentamause.cosydomainprovider.model.core.WatchtowerRiskLevel;
import com.magentamause.cosydomainprovider.repository.SubdomainRepository;
import com.magentamause.cosydomainprovider.repository.WatchtowerScanRepository;
import com.magentamause.cosydomainprovider.services.aws.ScreenshotStorageService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class WatchtowerServiceTest {

    @Mock private WatchtowerScanRepository scanRepository;
    @Mock private SubdomainRepository subdomainRepository;
    @Mock private ScreenshotStorageService screenshotStorage;

    private WatchtowerService service;

    @BeforeEach
    void setUp() {
        service = new WatchtowerService(scanRepository, subdomainRepository, screenshotStorage);
    }

    private static SubdomainEntity subdomain(String uuid, String label) {
        UserEntity owner = UserEntity.builder().uuid("owner-" + uuid).username("janne").build();
        return SubdomainEntity.builder()
                .uuid(uuid)
                .label(label)
                .fqdn(label + ".play.cosy-hosting.net")
                .owner(owner)
                .build();
    }

    private static WatchtowerScanEntity scan(
            String uuid, SubdomainEntity subdomain, WatchtowerCategory category) {
        return WatchtowerScanEntity.builder()
                .uuid(uuid)
                .subdomain(subdomain)
                .fqdn(subdomain.getFqdn())
                .scannedAt(Instant.parse("2026-08-09T03:12:00Z"))
                .reachable(true)
                .category(category)
                .riskLevel(
                        category == WatchtowerCategory.MALICIOUS
                                ? WatchtowerRiskLevel.HIGH
                                : WatchtowerRiskLevel.NONE)
                .summary("summary for " + uuid)
                .visitedPaths(List.of("/", "/login"))
                .modelId("claude-haiku-4-5-20251001")
                .reviewStatus(WatchtowerReviewStatus.PENDING)
                .build();
    }

    @Test
    void adminGetLatestScans_mapsScreenshotUrlFromStorage() {
        SubdomainEntity sd = subdomain("sd-1", "rich-crane");
        when(scanRepository.findLatestPerSubdomain())
                .thenReturn(List.of(scan("s-1", sd, WatchtowerCategory.COSY_FRONTEND)));
        when(screenshotStorage.presignedUrl(any())).thenReturn("https://minio/presigned");

        List<AdminWatchtowerScanDto> result = service.adminGetLatestScans();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getLabel()).isEqualTo("rich-crane");
        assertThat(result.getFirst().getOwnerUsername()).isEqualTo("janne");
        assertThat(result.getFirst().getScreenshotUrl()).isEqualTo("https://minio/presigned");
    }

    @Test
    void adminGetSummary_countsPerCategoryAndPendingReviews() {
        SubdomainEntity a = subdomain("sd-1", "rich-crane");
        SubdomainEntity b = subdomain("sd-2", "calm-otter");
        SubdomainEntity c = subdomain("sd-3", "swift-gecko");
        WatchtowerScanEntity flagged = scan("s-3", c, WatchtowerCategory.MALICIOUS);
        when(scanRepository.findLatestPerSubdomain())
                .thenReturn(
                        List.of(
                                scan("s-1", a, WatchtowerCategory.COSY_FRONTEND),
                                scan("s-2", b, WatchtowerCategory.BENIGN),
                                flagged));
        when(subdomainRepository.count()).thenReturn(4L);
        when(scanRepository.findFirstByOrderByScannedAtDesc()).thenReturn(Optional.of(flagged));

        AdminWatchtowerSummaryDto summary = service.adminGetSummary();

        assertThat(summary.getTotalSubdomains()).isEqualTo(4);
        assertThat(summary.getScannedSubdomains()).isEqualTo(3);
        assertThat(summary.getCosyFrontends()).isEqualTo(1);
        assertThat(summary.getBenign()).isEqualTo(1);
        assertThat(summary.getFlagged()).isEqualTo(1);
        assertThat(summary.getPendingReview()).isEqualTo(1);
        assertThat(summary.getLastScanAt()).isEqualTo(Instant.parse("2026-08-09T03:12:00Z"));
    }

    @Test
    void adminGetSummary_countsEmptySeparatelyFromBenignAndUnreachable() {
        SubdomainEntity a = subdomain("sd-1", "soft-cove");
        SubdomainEntity b = subdomain("sd-2", "calm-otter");
        SubdomainEntity c = subdomain("sd-3", "dead-host");
        WatchtowerScanEntity parked = scan("s-1", a, WatchtowerCategory.EMPTY);
        when(scanRepository.findLatestPerSubdomain())
                .thenReturn(
                        List.of(
                                parked,
                                scan("s-2", b, WatchtowerCategory.BENIGN),
                                scan("s-3", c, WatchtowerCategory.UNREACHABLE)));
        when(subdomainRepository.count()).thenReturn(3L);
        when(scanRepository.findFirstByOrderByScannedAtDesc()).thenReturn(Optional.of(parked));

        AdminWatchtowerSummaryDto summary = service.adminGetSummary();

        // A parked subdomain is neither content someone put there nor a dead host;
        // collapsing it into either would misreport how much of the estate is in use.
        assertThat(summary.getEmpty()).isEqualTo(1);
        assertThat(summary.getBenign()).isEqualTo(1);
        assertThat(summary.getUnreachable()).isEqualTo(1);
        assertThat(summary.getFlagged()).isZero();
    }

    @Test
    void adminGetSummary_dismissedFlagNoLongerCountsAsPending() {
        SubdomainEntity c = subdomain("sd-3", "swift-gecko");
        WatchtowerScanEntity flagged = scan("s-3", c, WatchtowerCategory.SUSPICIOUS);
        flagged.setReviewStatus(WatchtowerReviewStatus.DISMISSED);
        when(scanRepository.findLatestPerSubdomain()).thenReturn(List.of(flagged));
        when(subdomainRepository.count()).thenReturn(1L);
        when(scanRepository.findFirstByOrderByScannedAtDesc()).thenReturn(Optional.of(flagged));

        AdminWatchtowerSummaryDto summary = service.adminGetSummary();

        assertThat(summary.getFlagged()).isEqualTo(1);
        assertThat(summary.getPendingReview()).isZero();
    }

    @Test
    void ingestScan_persistsVerdictAsPendingReview() {
        SubdomainEntity sd = subdomain("sd-1", "swift-gecko");
        when(subdomainRepository.findById("sd-1")).thenReturn(Optional.of(sd));
        when(scanRepository.save(any(WatchtowerScanEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AdminWatchtowerScanDto result =
                service.ingestScan(
                        WatchtowerScanIngestDto.builder()
                                .subdomainUuid("sd-1")
                                .scannedAt(Instant.parse("2026-08-09T03:12:00Z"))
                                .reachable(true)
                                .httpStatus(200)
                                .category(WatchtowerCategory.MALICIOUS)
                                .riskLevel(WatchtowerRiskLevel.HIGH)
                                .summary("Crypto scam pattern")
                                .visitedPaths(List.of("/", "/wallet"))
                                .screenshotKey("2026-08-09/swift-gecko.png")
                                .modelId("claude-haiku-4-5-20251001")
                                .build());

        assertThat(result.getReviewStatus()).isEqualTo(WatchtowerReviewStatus.PENDING);
        assertThat(result.getVisitedPaths()).containsExactly("/", "/wallet");
        assertThat(result.getFqdn()).isEqualTo("swift-gecko.play.cosy-hosting.net");
    }

    @Test
    void ingestScan_unknownSubdomain_throwsNotFound() {
        when(subdomainRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.ingestScan(
                                        WatchtowerScanIngestDto.builder()
                                                .subdomainUuid("missing")
                                                .scannedAt(Instant.now())
                                                .reachable(true)
                                                .category(WatchtowerCategory.BENIGN)
                                                .riskLevel(WatchtowerRiskLevel.NONE)
                                                .summary("x")
                                                .modelId("m")
                                                .build()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Subdomain not found");
        verify(scanRepository, never()).save(any());
    }

    @Test
    void adminUpdateReview_setsStatusNoteAndTimestamp() {
        SubdomainEntity sd = subdomain("sd-1", "swift-gecko");
        WatchtowerScanEntity existing = scan("s-1", sd, WatchtowerCategory.SUSPICIOUS);
        when(scanRepository.findById("s-1")).thenReturn(Optional.of(existing));
        when(scanRepository.save(any(WatchtowerScanEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AdminWatchtowerScanDto result =
                service.adminUpdateReview(
                        "s-1",
                        WatchtowerReviewUpdateDto.builder()
                                .reviewStatus(WatchtowerReviewStatus.DISMISSED)
                                .reviewNote("False positive — it's a parody site")
                                .build());

        assertThat(result.getReviewStatus()).isEqualTo(WatchtowerReviewStatus.DISMISSED);
        assertThat(result.getReviewNote()).isEqualTo("False positive — it's a parody site");
        assertThat(result.getReviewedAt()).isNotNull();
    }

    @Test
    void adminGetScanHistory_unknownSubdomain_throwsNotFound() {
        when(subdomainRepository.existsById("missing")).thenReturn(false);

        assertThatThrownBy(() -> service.adminGetScanHistory("missing"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Subdomain not found");
    }
}
