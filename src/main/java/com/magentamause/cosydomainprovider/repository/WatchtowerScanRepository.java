package com.magentamause.cosydomainprovider.repository;

import com.magentamause.cosydomainprovider.entity.WatchtowerScanEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WatchtowerScanRepository extends JpaRepository<WatchtowerScanEntity, String> {

    /**
     * The newest scan per subdomain, newest first. The correlated subquery keeps this to a single
     * round trip instead of one query per subdomain.
     */
    @Query(
            """
            select s from WatchtowerScanEntity s
            where s.scannedAt = (
                select max(prior.scannedAt) from WatchtowerScanEntity prior
                where prior.subdomain = s.subdomain
            )
            order by s.scannedAt desc
            """)
    List<WatchtowerScanEntity> findLatestPerSubdomain();

    List<WatchtowerScanEntity> findAllBySubdomain_UuidOrderByScannedAtDesc(String subdomainUuid);

    Optional<WatchtowerScanEntity> findFirstByOrderByScannedAtDesc();

    /**
     * Drops a subdomain's scan history so the subdomain row itself can be deleted — the scans hold
     * a non-null FK to it. Derived (load-then-delete) on purpose: a bulk delete query would leave
     * the {@code watchtower_scan_visited_path} rows of the element collection behind.
     */
    void deleteAllBySubdomain_Uuid(String subdomainUuid);
}
