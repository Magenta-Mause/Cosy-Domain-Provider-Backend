package com.magentamause.cosydomainprovider.controller.v1.implementation;

import com.magentamause.cosydomainprovider.controller.v1.schema.SubdomainApi;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.model.action.SubdomainCreationDto;
import com.magentamause.cosydomainprovider.model.action.SubdomainUpdateDto;
import com.magentamause.cosydomainprovider.model.core.SubdomainDto;
import com.magentamause.cosydomainprovider.services.auth.SecurityContextService;
import com.magentamause.cosydomainprovider.services.core.SubdomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SubdomainController implements SubdomainApi {

    private final SubdomainService subdomainService;
    private final SecurityContextService securityContextService;

    @Override
    public ResponseEntity<List<SubdomainDto>> listMySubdomains() {
        UserEntity owner = securityContextService.getUser();
        String parentDomain = subdomainService.getParentDomain();
        List<SubdomainDto> dtos =
                subdomainService.getSubdomainsForOwner(owner).stream()
                        .map(s -> s.toDto(parentDomain))
                        .toList();
        return ResponseEntity.ok(dtos);
    }

    @Override
    public ResponseEntity<SubdomainDto> getSubdomain(String uuid) {
        UserEntity owner = securityContextService.getUser();
        return ResponseEntity.ok(
                subdomainService
                        .getOwnedSubdomain(uuid, owner)
                        .toDto(subdomainService.getParentDomain()));
    }

    @Override
    public ResponseEntity<SubdomainDto> createSubdomain(SubdomainCreationDto creationDto) {
        UserEntity owner = securityContextService.getUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        subdomainService
                                .createSubdomain(creationDto, owner)
                                .toDto(subdomainService.getParentDomain()));
    }

    @Override
    public ResponseEntity<SubdomainDto> updateSubdomain(String uuid, SubdomainUpdateDto updateDto) {
        UserEntity owner = securityContextService.getUser();
        return ResponseEntity.ok(
                subdomainService
                        .updateTargetIp(uuid, updateDto, owner)
                        .toDto(subdomainService.getParentDomain()));
    }

    @Override
    public ResponseEntity<Void> deleteSubdomain(String uuid) {
        UserEntity owner = securityContextService.getUser();
        subdomainService.deleteSubdomain(uuid, owner);
        return ResponseEntity.noContent().build();
    }
}
