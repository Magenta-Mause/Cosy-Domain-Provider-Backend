package com.magentamause.cosydomainprovider.services.core;

import com.magentamause.cosydomainprovider.entity.SubdomainEntity;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class WatchtowerService {

    /** Categories that put the parent domain's reputation at risk and need a human decision. */
    private static final Set<WatchtowerCategory> FLAGGED_CATEGORIES =
            Set.of(WatchtowerCategory.SUSPICIOUS, WatchtowerCategory.MALICIOUS);

    private final WatchtowerScanRepository scanRepository;
    private final SubdomainRepository subdomainRepository;
    private final ScreenshotStorageService screenshotStorage;

    /** Newest scan per subdomain, newest first. This is what the dashboard grid renders. */
    @Transactional(readOnly = true)
    public List<AdminWatchtowerScanDto> adminGetLatestScans() {
        return scanRepository.findLatestPerSubdomain().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public AdminWatchtowerScanDto adminGetScan(String uuid) {
        return toDto(requireScan(uuid));
    }

    /** Full scan history for one subdomain, newest first. */
    @Transactional(readOnly = true)
    public List<AdminWatchtowerScanDto> adminGetScanHistory(String subdomainUuid) {
        if (!subdomainRepository.existsById(subdomainUuid)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Subdomain not found");
        }
        return scanRepository.findAllBySubdomain_UuidOrderByScannedAtDesc(subdomainUuid).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminWatchtowerSummaryDto adminGetSummary() {
        List<WatchtowerScanEntity> latest = scanRepository.findLatestPerSubdomain();
        return AdminWatchtowerSummaryDto.builder()
                .totalSubdomains(subdomainRepository.count())
                .scannedSubdomains(latest.size())
                .cosyFrontends(countByCategory(latest, WatchtowerCategory.COSY_FRONTEND))
                .benign(countByCategory(latest, WatchtowerCategory.BENIGN))
                .flagged(
                        latest.stream()
                                .filter(s -> FLAGGED_CATEGORIES.contains(s.getCategory()))
                                .count())
                .unreachable(countByCategory(latest, WatchtowerCategory.UNREACHABLE))
                .pendingReview(
                        latest.stream()
                                .filter(s -> FLAGGED_CATEGORIES.contains(s.getCategory()))
                                .filter(s -> s.getReviewStatus() == WatchtowerReviewStatus.PENDING)
                                .count())
                .lastScanAt(
                        scanRepository
                                .findFirstByOrderByScannedAtDesc()
                                .map(WatchtowerScanEntity::getScannedAt)
                                .orElse(null))
                .build();
    }

    /** Stores one verdict from the nightly scanner. */
    @Transactional
    public AdminWatchtowerScanDto ingestScan(WatchtowerScanIngestDto dto) {
        SubdomainEntity subdomain =
                subdomainRepository
                        .findById(dto.getSubdomainUuid())
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND, "Subdomain not found"));

        WatchtowerScanEntity scan =
                WatchtowerScanEntity.builder()
                        .subdomain(subdomain)
                        .fqdn(
                                subdomain.getFqdn() != null
                                        ? subdomain.getFqdn()
                                        : subdomain.getLabel())
                        .scannedAt(dto.getScannedAt())
                        .reachable(Boolean.TRUE.equals(dto.getReachable()))
                        .httpStatus(dto.getHttpStatus())
                        .category(dto.getCategory())
                        .riskLevel(dto.getRiskLevel())
                        .summary(dto.getSummary())
                        .visitedPaths(new ArrayList<>(dto.getVisitedPaths()))
                        .screenshotKey(dto.getScreenshotKey())
                        .modelId(dto.getModelId())
                        .reviewStatus(WatchtowerReviewStatus.PENDING)
                        .build();

        WatchtowerScanEntity saved = scanRepository.save(scan);
        if (FLAGGED_CATEGORIES.contains(saved.getCategory())) {
            log.warn(
                    "Watchtower flagged {} as {} (risk {}): {}",
                    saved.getFqdn(),
                    saved.getCategory(),
                    saved.getRiskLevel(),
                    saved.getSummary());
        }
        return toDto(saved);
    }

    @Transactional
    public AdminWatchtowerScanDto adminUpdateReview(String uuid, WatchtowerReviewUpdateDto dto) {
        WatchtowerScanEntity scan = requireScan(uuid);
        scan.setReviewStatus(dto.getReviewStatus());
        scan.setReviewNote(dto.getReviewNote());
        scan.setReviewedAt(Instant.now());
        return toDto(scanRepository.save(scan));
    }

    private WatchtowerScanEntity requireScan(String uuid) {
        return scanRepository
                .findById(uuid)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scan not found"));
    }

    private static long countByCategory(
            List<WatchtowerScanEntity> scans, WatchtowerCategory category) {
        return scans.stream().filter(s -> s.getCategory() == category).count();
    }

    private AdminWatchtowerScanDto toDto(WatchtowerScanEntity scan) {
        SubdomainEntity subdomain = scan.getSubdomain();
        return AdminWatchtowerScanDto.builder()
                .uuid(scan.getUuid())
                .subdomainUuid(subdomain.getUuid())
                .label(subdomain.getLabel())
                .fqdn(scan.getFqdn())
                .ownerUuid(subdomain.getOwner().getUuid())
                .ownerUsername(subdomain.getOwner().getUsername())
                .scannedAt(scan.getScannedAt())
                .reachable(scan.isReachable())
                .httpStatus(scan.getHttpStatus())
                .category(scan.getCategory())
                .riskLevel(
                        scan.getRiskLevel() != null
                                ? scan.getRiskLevel()
                                : WatchtowerRiskLevel.NONE)
                .summary(scan.getSummary())
                .visitedPaths(List.copyOf(scan.getVisitedPaths()))
                .screenshotUrl(screenshotStorage.presignedUrl(scan.getScreenshotKey()))
                .modelId(scan.getModelId())
                .reviewStatus(scan.getReviewStatus())
                .reviewNote(scan.getReviewNote())
                .reviewedAt(scan.getReviewedAt())
                .build();
    }
}
