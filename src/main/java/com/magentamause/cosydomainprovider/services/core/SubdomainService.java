package com.magentamause.cosydomainprovider.services.core;

import com.magentamause.cosydomainprovider.configuration.aws.Route53Properties;
import com.magentamause.cosydomainprovider.configuration.subdomain.SubdomainProperties;
import com.magentamause.cosydomainprovider.entity.SubdomainEntity;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.model.action.SubdomainCreationDto;
import com.magentamause.cosydomainprovider.model.action.SubdomainUpdateDto;
import com.magentamause.cosydomainprovider.model.core.LabelAvailabilityDto;
import com.magentamause.cosydomainprovider.model.core.LabelMode;
import com.magentamause.cosydomainprovider.model.core.Plan;
import com.magentamause.cosydomainprovider.model.core.SubdomainStatus;
import com.magentamause.cosydomainprovider.model.dns.DnsRecordType;
import com.magentamause.cosydomainprovider.model.exception.LabelConflictException;
import com.magentamause.cosydomainprovider.model.exception.SubdomainNotFoundException;
import com.magentamause.cosydomainprovider.model.exception.SubdomainQuotaExceededException;
import com.magentamause.cosydomainprovider.repository.SubdomainRepository;
import com.magentamause.cosydomainprovider.repository.WatchtowerScanRepository;
import com.magentamause.cosydomainprovider.services.aws.Route53Service;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.route53.model.InvalidChangeBatchException;

/**
 * TODO: add a DuckDNS-style dynamic update endpoint (GET /update?label=...&token=...&ip=...) using
 * a per-subdomain token so dynamic DNS clients can push IP changes without a full login.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubdomainService {

    private static final int MAX_LABEL_ATTEMPTS = 25;
    private static final int MIN_LABEL_LENGTH = 3;
    private static final int MAX_LABEL_LENGTH = 63;
    private static final String LABEL_PATTERN = "[a-z0-9-]+";
    private static final String VERB_UPDATED = "Updated";
    private static final String VERB_CREATED = "Created";
    private static final String VERB_RELABELED = "Relabeled";
    private static final String ACTOR_ADMIN = "admin";

    private final SubdomainRepository subdomainRepository;
    private final WatchtowerScanRepository watchtowerScanRepository;
    private final Route53Service route53Service;
    private final SubdomainProperties subdomainProperties;
    private final Route53Properties route53Properties;
    private final SubdomainNameGenerator nameGenerator;
    private final GlobalSettingsService globalSettingsService;

    public List<SubdomainEntity> getSubdomainsForOwner(UserEntity owner) {
        return subdomainRepository.findAllByOwner(owner);
    }

    public SubdomainEntity getOwnedSubdomain(String uuid, UserEntity owner) {
        SubdomainEntity entity =
                subdomainRepository
                        .findById(uuid)
                        .orElseThrow(() -> new SubdomainNotFoundException(uuid));
        if (!entity.getOwner().getUuid().equals(owner.getUuid())) {
            throw new SubdomainNotFoundException(uuid);
        }
        return entity;
    }

    public SubdomainEntity createSubdomain(SubdomainCreationDto dto, UserEntity owner) {
        if (!globalSettingsService.isDomainCreationEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Domain creation is currently disabled by admin");
        }
        if (!owner.isVerified()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "User must be verified to create subdomains");
        }

        boolean useCustomLabel =
                owner.getPlan() == Plan.PLUS && dto.getLabel() != null && !dto.getLabel().isBlank();
        String label =
                useCustomLabel ? dto.getLabel().toLowerCase(Locale.ROOT) : generateUniqueLabel();
        LabelMode labelMode = useCustomLabel ? LabelMode.CUSTOM : LabelMode.RANDOM;
        validateLabel(label);

        long ownedCount = subdomainRepository.countByOwner(owner);
        int limit =
                owner.computeMaxSubdomainCount(
                        subdomainProperties.getMaxPerFreeUser(),
                        subdomainProperties.getMaxPerPlusUser());
        if (ownedCount >= limit) {
            throw new SubdomainQuotaExceededException(limit);
        }

        String fqdn = label + "." + route53Properties.getDomain();
        SubdomainEntity entity =
                SubdomainEntity.builder()
                        .label(label)
                        .fqdn(fqdn)
                        .owner(owner)
                        .targetIp(dto.getTargetIp())
                        .targetIpv6(dto.getTargetIpv6())
                        .status(SubdomainStatus.PENDING)
                        .labelMode(labelMode)
                        .build();
        entity = subdomainRepository.save(entity);
        entity =
                syncDnsRecord(
                        entity,
                        dto.getTargetIp(),
                        DnsRecordType.A,
                        VERB_CREATED,
                        owner.getUuid(),
                        route53Service::upsertARecord);
        entity =
                syncDnsRecord(
                        entity,
                        dto.getTargetIpv6(),
                        DnsRecordType.AAAA,
                        VERB_CREATED,
                        owner.getUuid(),
                        route53Service::upsertAAAARecord);
        return entity;
    }

    public SubdomainEntity updateTargetIp(String uuid, SubdomainUpdateDto dto, UserEntity owner) {
        return applyTargetIpUpdate(getOwnedSubdomain(uuid, owner), dto, owner.getUuid());
    }

    public void adminDeleteSubdomain(String uuid) {
        SubdomainEntity entity =
                subdomainRepository
                        .findById(uuid)
                        .orElseThrow(() -> new SubdomainNotFoundException(uuid));
        deleteSubdomain(entity);
    }

    public void deleteSubdomain(String uuid, UserEntity owner) {
        deleteSubdomain(getOwnedSubdomain(uuid, owner));
    }

    public LabelAvailabilityDto checkLabelAvailability(String label) {
        String normalized = label.toLowerCase(Locale.ROOT);
        if (normalized.length() < MIN_LABEL_LENGTH) {
            return LabelAvailabilityDto.unavailable("too short");
        }
        if (normalized.length() > MAX_LABEL_LENGTH) {
            return LabelAvailabilityDto.unavailable("too long");
        }
        if (!normalized.matches(LABEL_PATTERN)) {
            return LabelAvailabilityDto.unavailable(
                    "must only contain lowercase letters, digits, and hyphens");
        }
        try {
            validateLabel(normalized);
        } catch (LabelConflictException e) {
            return LabelAvailabilityDto.unavailable(e.getMessage());
        }
        return LabelAvailabilityDto.available();
    }

    public String getParentDomain() {
        return route53Properties.getDomain();
    }

    public long getDefaultTtl() {
        return route53Properties.getDefaultTtl();
    }

    public List<SubdomainEntity> adminGetAllSubdomains() {
        return subdomainRepository.findAll();
    }

    public SubdomainEntity adminGetSubdomain(String uuid) {
        return subdomainRepository
                .findById(uuid)
                .orElseThrow(() -> new SubdomainNotFoundException(uuid));
    }

    public long getCountByOwner(UserEntity owner) {
        return subdomainRepository.countByOwner(owner);
    }

    public SubdomainEntity adminUpdateTargetIp(String uuid, SubdomainUpdateDto dto) {
        SubdomainEntity entity =
                subdomainRepository
                        .findById(uuid)
                        .orElseThrow(() -> new SubdomainNotFoundException(uuid));
        return applyTargetIpUpdate(entity, dto, ACTOR_ADMIN);
    }

    private SubdomainEntity applyTargetIpUpdate(
            SubdomainEntity entity, SubdomainUpdateDto dto, Object actor) {
        String oldIp = entity.getTargetIp();
        String oldIpv6 = entity.getTargetIpv6();
        entity.setTargetIp(dto.getTargetIp());
        entity.setTargetIpv6(dto.getTargetIpv6());
        entity.setStatus(SubdomainStatus.PENDING);
        entity = subdomainRepository.save(entity);
        entity = removeClearedDnsRecord(entity, oldIp, dto.getTargetIp(), DnsRecordType.A);
        entity = removeClearedDnsRecord(entity, oldIpv6, dto.getTargetIpv6(), DnsRecordType.AAAA);
        entity =
                syncDnsRecord(
                        entity,
                        dto.getTargetIp(),
                        DnsRecordType.A,
                        VERB_UPDATED,
                        actor,
                        route53Service::upsertARecord);
        entity =
                syncDnsRecord(
                        entity,
                        dto.getTargetIpv6(),
                        DnsRecordType.AAAA,
                        VERB_UPDATED,
                        actor,
                        route53Service::upsertAAAARecord);
        return entity;
    }

    private SubdomainEntity removeClearedDnsRecord(
            SubdomainEntity entity, String oldIp, String newIp, DnsRecordType recordType) {
        if (oldIp == null || oldIp.isBlank()) return entity;
        if (newIp != null && !newIp.isBlank()) return entity;
        String fqdn = fqdnOf(entity);
        try {
            deleteDnsRecord(fqdn, oldIp, recordType);
            log.info("Removed {} record {} -> {}", recordType, fqdn, oldIp);
        } catch (Exception e) {
            entity.setStatus(SubdomainStatus.FAILED);
            entity = subdomainRepository.save(entity);
            log.error(
                    "Route53 {} delete failed for {} -> {}: {}",
                    recordType,
                    fqdn,
                    oldIp,
                    e.getMessage(),
                    e);
        }
        return entity;
    }

    public SubdomainEntity adminRelabelSubdomain(String uuid, String newLabel) {
        SubdomainEntity entity =
                subdomainRepository
                        .findById(uuid)
                        .orElseThrow(() -> new SubdomainNotFoundException(uuid));
        String normalized = newLabel.toLowerCase(Locale.ROOT);
        if (normalized.equalsIgnoreCase(entity.getLabel())) {
            return entity;
        }
        if (subdomainProperties.getReservedLabels().stream()
                .anyMatch(normalized::equalsIgnoreCase)) {
            throw LabelConflictException.reserved(normalized);
        }
        subdomainRepository
                .findByLabelIgnoreCase(normalized)
                .filter(existing -> !existing.getUuid().equals(uuid))
                .ifPresent(
                        existing -> {
                            throw LabelConflictException.taken(normalized);
                        });

        String oldFqdn = fqdnOf(entity);
        try {
            if (entity.getTargetIp() != null && !entity.getTargetIp().isBlank()) {
                route53Service.deleteARecord(oldFqdn, entity.getTargetIp());
            }
            if (entity.getTargetIpv6() != null && !entity.getTargetIpv6().isBlank()) {
                route53Service.deleteAAAARecord(oldFqdn, entity.getTargetIpv6());
            }
        } catch (Exception e) {
            log.error(
                    "Route53 delete failed for old FQDN {} during relabel: {}",
                    oldFqdn,
                    e.getMessage(),
                    e);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Failed to remove old DNS records; relabel aborted");
        }

        String newFqdn = normalized + "." + route53Properties.getDomain();
        entity.setLabel(normalized);
        entity.setFqdn(newFqdn);
        entity.setStatus(SubdomainStatus.PENDING);
        entity = subdomainRepository.save(entity);
        entity =
                syncDnsRecord(
                        entity,
                        entity.getTargetIp(),
                        DnsRecordType.A,
                        VERB_RELABELED,
                        ACTOR_ADMIN,
                        route53Service::upsertARecord);
        entity =
                syncDnsRecord(
                        entity,
                        entity.getTargetIpv6(),
                        DnsRecordType.AAAA,
                        VERB_RELABELED,
                        ACTOR_ADMIN,
                        route53Service::upsertAAAARecord);
        return entity;
    }

    public void deleteSubdomainsByOwner(String uuid) {
        subdomainRepository.findAllByOwner_Uuid(uuid).forEach(this::deleteSubdomain);
    }

    private String generateUniqueLabel() {
        for (int i = 0; i < MAX_LABEL_ATTEMPTS; i++) {
            String candidate = nameGenerator.generate();
            boolean reserved =
                    subdomainProperties.getReservedLabels().stream()
                            .anyMatch(candidate::equalsIgnoreCase);
            if (!reserved && subdomainRepository.findByLabelIgnoreCase(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "No subdomain label available; pool of "
                        + nameGenerator.poolSize()
                        + " names is exhausted");
    }

    private void validateLabel(String label) {
        if (subdomainProperties.getReservedLabels().stream().anyMatch(label::equalsIgnoreCase)) {
            throw LabelConflictException.reserved(label);
        }
        if (subdomainRepository.findByLabelIgnoreCase(label).isPresent()) {
            throw LabelConflictException.taken(label);
        }
    }

    private SubdomainEntity syncDnsRecord(
            SubdomainEntity entity,
            String ip,
            DnsRecordType recordType,
            String verb,
            Object ownerUuid,
            BiConsumer<String, String> upsert) {
        if (ip == null || ip.isBlank()) return entity;
        String fqdn = fqdnOf(entity);
        try {
            upsert.accept(fqdn, ip);
            entity.setStatus(SubdomainStatus.ACTIVE);
            log.info("{} {} record {} -> {} for user {}", verb, recordType, fqdn, ip, ownerUuid);
        } catch (Exception e) {
            entity.setStatus(SubdomainStatus.FAILED);
            log.error(
                    "Route53 {} upsert failed for {} -> {}: {}",
                    recordType,
                    fqdn,
                    ip,
                    e.getMessage(),
                    e);
        }
        return subdomainRepository.save(entity);
    }

    private void deleteSubdomain(SubdomainEntity entity) {
        String fqdn = fqdnOf(entity);
        // Only the Route53 calls are guarded: a DNS failure is retryable and the row is kept.
        // Anything that goes wrong locally afterwards is our bug, not the registrar's, and must
        // not be reported to the user as "failed to remove DNS record".
        try {
            if (entity.getTargetIp() != null && !entity.getTargetIp().isBlank()) {
                deleteDnsRecord(fqdn, entity.getTargetIp(), DnsRecordType.A);
            }
            if (entity.getTargetIpv6() != null && !entity.getTargetIpv6().isBlank()) {
                deleteDnsRecord(fqdn, entity.getTargetIpv6(), DnsRecordType.AAAA);
            }
        } catch (Exception e) {
            entity.setStatus(SubdomainStatus.FAILED);
            subdomainRepository.save(entity);
            log.error("Route53 delete failed for {}: {}", fqdn, e.getMessage(), e);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to remove DNS record; subdomain marked FAILED and retained for retry");
        }
        // Watchtower scans hold a non-null FK to the subdomain, so the history goes first.
        watchtowerScanRepository.deleteAllBySubdomain_Uuid(entity.getUuid());
        subdomainRepository.delete(entity);
        log.info("Deleted subdomain {}", fqdn);
    }

    private void deleteDnsRecord(String fqdn, String ip, DnsRecordType recordType) {
        try {
            if (recordType == DnsRecordType.AAAA) {
                route53Service.deleteAAAARecord(fqdn, ip);
            } else {
                route53Service.deleteARecord(fqdn, ip);
            }
        } catch (InvalidChangeBatchException e) {
            log.warn(
                    "DNS record {} not found in Route53 during delete (already absent), skipping",
                    fqdn);
        }
    }

    private String fqdnOf(SubdomainEntity entity) {
        if (entity.getFqdn() != null) {
            return entity.getFqdn();
        }
        log.warn(
                "Subdomain {} has no stored FQDN; falling back to current domain config",
                entity.getUuid());
        return entity.getLabel() + "." + route53Properties.getDomain();
    }
}
