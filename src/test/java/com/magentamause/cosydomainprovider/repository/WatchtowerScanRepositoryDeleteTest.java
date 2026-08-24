package com.magentamause.cosydomainprovider.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.magentamause.cosydomainprovider.entity.SubdomainEntity;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.entity.WatchtowerScanEntity;
import com.magentamause.cosydomainprovider.model.core.SubdomainStatus;
import com.magentamause.cosydomainprovider.model.core.WatchtowerCategory;
import com.magentamause.cosydomainprovider.model.core.WatchtowerRiskLevel;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deleting a subdomain that Watchtower had scanned used to fail on the scans' foreign key. The
 * service-level test mocks the repositories, so only a real EntityManager proves the two deletes
 * actually work in the order and the transaction context the HTTP request gives them.
 *
 * <p>{@code NOT_SUPPORTED} is the whole point: {@code @DataJpaTest} wraps each test in a
 * transaction by default, which the derived delete would silently join — exactly the ambient
 * transaction a controller thread does <em>not</em> have.
 */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class WatchtowerScanRepositoryDeleteTest {

    @Autowired private WatchtowerScanRepository watchtowerScanRepository;
    @Autowired private SubdomainRepository subdomainRepository;
    @Autowired private UserRepository userRepository;

    @Test
    void deletingScansThenSubdomainSucceedsWithoutAnAmbientTransaction() {
        UserEntity owner =
                userRepository.save(
                        UserEntity.builder()
                                .username("alice")
                                .email("alice@example.com")
                                .isVerified(true)
                                .build());
        SubdomainEntity subdomain =
                subdomainRepository.save(
                        SubdomainEntity.builder()
                                .label("earthy-finch")
                                .fqdn("earthy-finch.test.example.com")
                                .owner(owner)
                                .targetIp("1.2.3.4")
                                .status(SubdomainStatus.ACTIVE)
                                .build());
        watchtowerScanRepository.save(
                WatchtowerScanEntity.builder()
                        .subdomain(subdomain)
                        .fqdn(subdomain.getFqdn())
                        .scannedAt(Instant.now())
                        .reachable(true)
                        .category(WatchtowerCategory.EMPTY)
                        .riskLevel(WatchtowerRiskLevel.LOW)
                        .summary("A parked page.")
                        .visitedPaths(List.of("/", "/about"))
                        .modelId("claude-haiku-4-5-20251001")
                        .build());

        watchtowerScanRepository.deleteAllBySubdomain_Uuid(subdomain.getUuid());
        assertThatCode(() -> subdomainRepository.delete(subdomain)).doesNotThrowAnyException();

        assertThat(subdomainRepository.findById(subdomain.getUuid())).isEmpty();
        assertThat(
                        watchtowerScanRepository.findAllBySubdomain_UuidOrderByScannedAtDesc(
                                subdomain.getUuid()))
                .isEmpty();
    }
}
