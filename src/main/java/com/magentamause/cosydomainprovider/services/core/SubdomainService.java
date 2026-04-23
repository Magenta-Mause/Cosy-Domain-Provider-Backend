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
import java.util.UUID;

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

    private final SubdomainRepository subdomainRepository;
    private final Route53Service route53Service;
    private final SubdomainProperties subdomainProperties;
    private final Route53Properties route53Properties;

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
            // Don't leak existence to non-owners.
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Subdomain " + uuid + " not found");
        }
        return entity;
    }

    private boolean checkCanUserCreateSubdomain(UserEntity user) {
        return user.isVerified();
    }

    public SubdomainEntity createSubdomain(SubdomainCreationDto dto, UserEntity owner) {
        String label = owner.getPlan() == Plan.PLUS
                ? dto.getLabel().toLowerCase(Locale.ROOT)
                : UUID.randomUUID().toString().substring(0, 8);
        if (!checkCanUserCreateSubdomain(owner)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User must be verified to create subdomains");
        }

        if (subdomainProperties.getReservedLabels().stream().anyMatch(label::equalsIgnoreCase)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Label '" + label + "' is reserved");
        }

        long ownedCount = subdomainRepository.countByOwner(owner);
        int limit = owner.getPlan() == Plan.PLUS
                ? subdomainProperties.getMaxPerPlusUser()
                : subdomainProperties.getMaxPerFreeUser();
        if (ownedCount >= limit) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Subdomain quota reached (" + limit + " per user)");
        }

        if (subdomainRepository.findByLabelIgnoreCase(label).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Label '" + label + "' is already taken");
        }

        SubdomainEntity entity =
                SubdomainEntity.builder()
                        .label(label)
                        .owner(owner)
                        .targetIp(dto.getTargetIp())
                        .status(SubdomainStatus.PENDING)
                        .build();
        entity = subdomainRepository.save(entity);

        try {
            route53Service.upsertARecord(label, dto.getTargetIp());
            entity.setStatus(SubdomainStatus.ACTIVE);
            log.info(
                    "Created subdomain {}.{} -> {} for user {}",
                    label,
                    route53Properties.getDomain(),
                    dto.getTargetIp(),
                    owner.getUuid());
        } catch (Exception e) {
            entity.setStatus(SubdomainStatus.FAILED);
            log.error(
                    "Route53 upsert failed for {}.{} -> {}: {}",
                    label,
                    route53Properties.getDomain(),
                    dto.getTargetIp(),
                    e.getMessage(),
                    e);
        }
        return subdomainRepository.save(entity);
    }

    public SubdomainEntity updateTargetIp(String uuid, SubdomainUpdateDto dto, UserEntity owner) {
        SubdomainEntity entity = getOwnedSubdomain(uuid, owner);
        entity.setTargetIp(dto.getTargetIp());
        entity.setStatus(SubdomainStatus.PENDING);
        entity = subdomainRepository.save(entity);

        try {
            route53Service.upsertARecord(entity.getLabel(), dto.getTargetIp());
            entity.setStatus(SubdomainStatus.ACTIVE);
            log.info(
                    "Updated subdomain {}.{} -> {} for user {}",
                    entity.getLabel(),
                    route53Properties.getDomain(),
                    dto.getTargetIp(),
                    owner.getUuid());
        } catch (Exception e) {
            entity.setStatus(SubdomainStatus.FAILED);
            log.error(
                    "Route53 update failed for {}.{} -> {}: {}",
                    entity.getLabel(),
                    route53Properties.getDomain(),
                    dto.getTargetIp(),
                    e.getMessage(),
                    e);
        }
        return subdomainRepository.save(entity);
    }

    public void deleteSubdomain(String uuid, UserEntity owner) {
        SubdomainEntity entity = getOwnedSubdomain(uuid, owner);
        try {
            route53Service.deleteARecord(entity.getLabel(), entity.getTargetIp());
            subdomainRepository.delete(entity);
            log.info(
                    "Deleted subdomain {}.{} for user {}",
                    entity.getLabel(),
                    route53Properties.getDomain(),
                    owner.getUuid());
        } catch (Exception e) {
            entity.setStatus(SubdomainStatus.FAILED);
            subdomainRepository.save(entity);
            log.error(
                    "Route53 delete failed for {}.{}: {}",
                    entity.getLabel(),
                    route53Properties.getDomain(),
                    e.getMessage(),
                    e);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to remove DNS record; subdomain marked FAILED and retained for retry");
        }
    }

    public LabelAvailabilityDto checkLabelAvailability(String label) {
        String normalized = label.toLowerCase(Locale.ROOT);
        if (subdomainProperties.getReservedLabels().stream().anyMatch(normalized::equalsIgnoreCase)) {
            return LabelAvailabilityDto.unavailable("reserved");
        }
        if (subdomainRepository.findByLabelIgnoreCase(normalized).isPresent()) {
            return LabelAvailabilityDto.unavailable("taken");
        }
        return LabelAvailabilityDto.available();
    }

    public String getParentDomain() {
        return route53Properties.getDomain();
    }
}
