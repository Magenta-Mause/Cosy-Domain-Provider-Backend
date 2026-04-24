package com.magentamause.cosydomainprovider.services.core;

import com.magentamause.cosydomainprovider.configuration.aws.Route53Properties;
import com.magentamause.cosydomainprovider.configuration.subdomain.SubdomainProperties;
import com.magentamause.cosydomainprovider.entity.SubdomainEntity;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.model.action.SubdomainCreationDto;
import com.magentamause.cosydomainprovider.model.action.SubdomainUpdateDto;
import com.magentamause.cosydomainprovider.model.core.LabelAvailabilityDto;
import com.magentamause.cosydomainprovider.model.core.Plan;
import com.magentamause.cosydomainprovider.model.core.SubdomainStatus;
import com.magentamause.cosydomainprovider.repository.SubdomainRepository;
import com.magentamause.cosydomainprovider.services.aws.Route53Service;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * TODO: add a DuckDNS-style dynamic update endpoint (GET /update?label=...&token=...&ip=...) using
 * a per-subdomain token so dynamic DNS clients can push IP changes without a full login.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubdomainService {

    private static final int MAX_LABEL_ATTEMPTS = 25;

    private final SubdomainRepository subdomainRepository;
    private final Route53Service route53Service;
    private final SubdomainProperties subdomainProperties;
    private final Route53Properties route53Properties;
    private final SubdomainNameGenerator nameGenerator;

    public List<SubdomainEntity> getSubdomainsForOwner(UserEntity owner) {
        return subdomainRepository.findAllByOwner(owner);
    }

    public SubdomainEntity getOwnedSubdomain(String uuid, UserEntity owner) {
        SubdomainEntity entity =
                subdomainRepository
                        .findById(uuid)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Subdomain " + uuid + " not found"));
        if (!entity.getOwner().getUuid().equals(owner.getUuid())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Subdomain " + uuid + " not found");
        }
        return entity;
    }

    public SubdomainEntity createSubdomain(SubdomainCreationDto dto, UserEntity owner) {
        if (!owner.isVerified()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "User must be verified to create subdomains");
        }

        String label = resolveLabel(dto, owner);
        validateLabel(label);

        long ownedCount = subdomainRepository.countByOwner(owner);
        int limit =
                owner.getPlan() == Plan.PLUS
                        ? subdomainProperties.getMaxPerPlusUser()
                        : subdomainProperties.getMaxPerFreeUser();
        if (ownedCount >= limit) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Subdomain quota reached (" + limit + " per user)");
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
                        .build();
        entity = subdomainRepository.save(entity);
        entity = syncARecordIfPresent(entity, dto.getTargetIp(), "Created", owner.getUuid());
        entity = syncAAAARecordIfPresent(entity, dto.getTargetIpv6(), "Created", owner.getUuid());
        return entity;
    }

    public SubdomainEntity updateTargetIp(String uuid, SubdomainUpdateDto dto, UserEntity owner) {
        SubdomainEntity entity = getOwnedSubdomain(uuid, owner);
        entity.setTargetIp(dto.getTargetIp());
        entity.setTargetIpv6(dto.getTargetIpv6());
        entity.setStatus(SubdomainStatus.PENDING);
        entity = subdomainRepository.save(entity);
        entity = syncARecordIfPresent(entity, dto.getTargetIp(), "Updated", owner.getUuid());
        entity = syncAAAARecordIfPresent(entity, dto.getTargetIpv6(), "Updated", owner.getUuid());
        return entity;
    }

    public void deleteSubdomain(String uuid) {
        SubdomainEntity entity =
                subdomainRepository
                        .findById(uuid)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Subdomain " + uuid + " not found"));
        deleteSubdomain(entity);
    }

    public void deleteSubdomain(String uuid, UserEntity owner) {
        deleteSubdomain(getOwnedSubdomain(uuid, owner));
    }

    public LabelAvailabilityDto checkLabelAvailability(String label) {
        String normalized = label.toLowerCase(Locale.ROOT);
        if (normalized.length() < 3) {
            return LabelAvailabilityDto.unavailable("too short");
        }
        try {
            validateLabel(normalized);
        } catch (ResponseStatusException e) {
            return LabelAvailabilityDto.unavailable(e.getReason());
        }
        return LabelAvailabilityDto.available();
    }

    public String getParentDomain() {
        return route53Properties.getDomain();
    }

    public void deleteSubdomainsByOwner(String uuid) {
        subdomainRepository
                .findAllByOwner_Uuid(uuid)
                .forEach(subdomain -> deleteSubdomain(subdomain.getUuid()));
    }

    private String resolveLabel(SubdomainCreationDto dto, UserEntity owner) {
        if (owner.getPlan() == Plan.PLUS && dto.getLabel() != null && !dto.getLabel().isBlank()) {
            return dto.getLabel().toLowerCase(Locale.ROOT);
        }
        return generateUniqueLabel();
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
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Label '" + label + "' is reserved");
        }
        if (subdomainRepository.findByLabelIgnoreCase(label).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Label '" + label + "' is already taken");
        }
    }

    private SubdomainEntity syncARecordIfPresent(
            SubdomainEntity entity, String targetIp, String verb, Object ownerUuid) {
        if (targetIp == null || targetIp.isBlank()) return entity;
        String fqdn = fqdnOf(entity);
        try {
            route53Service.upsertARecord(fqdn, targetIp);
            entity.setStatus(SubdomainStatus.ACTIVE);
            log.info("{} A record {} -> {} for user {}", verb, fqdn, targetIp, ownerUuid);
        } catch (Exception e) {
            entity.setStatus(SubdomainStatus.FAILED);
            log.error(
                    "Route53 A upsert failed for {} -> {}: {}", fqdn, targetIp, e.getMessage(), e);
        }
        return subdomainRepository.save(entity);
    }

    private SubdomainEntity syncAAAARecordIfPresent(
            SubdomainEntity entity, String targetIpv6, String verb, Object ownerUuid) {
        if (targetIpv6 == null || targetIpv6.isBlank()) return entity;
        String fqdn = fqdnOf(entity);
        try {
            route53Service.upsertAAAARecord(fqdn, targetIpv6);
            entity.setStatus(SubdomainStatus.ACTIVE);
            log.info("{} AAAA record {} -> {} for user {}", verb, fqdn, targetIpv6, ownerUuid);
        } catch (Exception e) {
            entity.setStatus(SubdomainStatus.FAILED);
            log.error(
                    "Route53 AAAA upsert failed for {} -> {}: {}",
                    fqdn,
                    targetIpv6,
                    e.getMessage(),
                    e);
        }
        return subdomainRepository.save(entity);
    }

    private void deleteSubdomain(SubdomainEntity entity) {
        String fqdn = fqdnOf(entity);
        try {
            if (entity.getTargetIp() != null && !entity.getTargetIp().isBlank()) {
                route53Service.deleteARecord(fqdn, entity.getTargetIp());
            }
            if (entity.getTargetIpv6() != null && !entity.getTargetIpv6().isBlank()) {
                route53Service.deleteAAAARecord(fqdn, entity.getTargetIpv6());
            }
            subdomainRepository.delete(entity);
            log.info("Deleted subdomain {}", fqdn);
        } catch (Exception e) {
            entity.setStatus(SubdomainStatus.FAILED);
            subdomainRepository.save(entity);
            log.error("Route53 delete failed for {}: {}", fqdn, e.getMessage(), e);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to remove DNS record; subdomain marked FAILED and retained for retry");
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
