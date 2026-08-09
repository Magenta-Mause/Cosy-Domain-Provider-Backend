package com.magentamause.cosydomainprovider.entity;

import com.magentamause.cosydomainprovider.model.core.WatchtowerCategory;
import com.magentamause.cosydomainprovider.model.core.WatchtowerReviewStatus;
import com.magentamause.cosydomainprovider.model.core.WatchtowerRiskLevel;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

/**
 * One nightly Watchtower verdict for one subdomain. Rows are append-only — the dashboard reads the
 * newest row per subdomain, older rows stay as history.
 */
@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(indexes = {@Index(name = "idx_watchtower_scan_subdomain", columnList = "subdomain_uuid")})
public class WatchtowerScanEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String uuid;

    @ManyToOne(optional = false)
    @JoinColumn(name = "subdomain_uuid", nullable = false)
    private SubdomainEntity subdomain;

    /**
     * The FQDN as it was at scan time. Kept denormalised so a later relabel does not rewrite
     * history.
     */
    @Column(nullable = false)
    private String fqdn;

    @Column(nullable = false)
    private Instant scannedAt;

    @Column(nullable = false)
    private boolean reachable;

    @Column private Integer httpStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WatchtowerCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WatchtowerRiskLevel riskLevel;

    /** One or two sentences from the agent describing what the site does. */
    @Column(nullable = false, length = 2000)
    private String summary;

    /** Paths the agent actually opened, in visit order. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "watchtower_scan_visited_path",
            joinColumns = @JoinColumn(name = "scan_uuid"))
    @Column(name = "path", nullable = false)
    @Builder.Default
    private List<String> visitedPaths = new ArrayList<>();

    /** MinIO object key of the full-page screenshot, or null when the site was unreachable. */
    @Column private String screenshotKey;

    /** Which model produced the verdict, e.g. {@code claude-haiku-4-5-20251001}. */
    @Column(nullable = false)
    private String modelId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WatchtowerReviewStatus reviewStatus = WatchtowerReviewStatus.PENDING;

    @Column(length = 2000)
    private String reviewNote;

    @Column private Instant reviewedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
