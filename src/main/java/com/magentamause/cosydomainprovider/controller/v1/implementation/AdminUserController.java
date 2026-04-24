package com.magentamause.cosydomainprovider.controller.v1.implementation;

import com.magentamause.cosydomainprovider.configuration.admin.AdminProperties;
import com.magentamause.cosydomainprovider.configuration.subdomain.SubdomainProperties;
import com.magentamause.cosydomainprovider.controller.v1.schema.AdminApi;
import com.magentamause.cosydomainprovider.entity.SubdomainEntity;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.model.admin.AdminSubdomainDto;
import com.magentamause.cosydomainprovider.model.admin.AdminUserDetailDto;
import com.magentamause.cosydomainprovider.model.admin.AdminUserDto;
import com.magentamause.cosydomainprovider.model.core.UserDto;
import com.magentamause.cosydomainprovider.repository.SubdomainRepository;
import com.magentamause.cosydomainprovider.repository.UserRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class AdminUserController implements AdminApi {

    private final UserRepository userRepository;
    private final SubdomainRepository subdomainRepository;
    private final AdminProperties adminProperties;
    private final SubdomainProperties subdomainProperties;

    @Override
    public ResponseEntity<List<AdminSubdomainDto>> getAllSubdomains(String adminKey) {
        validateKey(adminKey);
        List<AdminSubdomainDto> dtos = subdomainRepository.findAll().stream()
                .map(this::toAdminSubdomainDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @Override
    public ResponseEntity<AdminSubdomainDto> getSubdomain(String adminKey, String uuid) {
        validateKey(adminKey);
        SubdomainEntity subdomain = subdomainRepository.findById(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subdomain not found"));
        return ResponseEntity.ok(toAdminSubdomainDto(subdomain));
    }

    @Override
    public ResponseEntity<List<AdminUserDto>> getAllUsers(String adminKey) {
        validateKey(adminKey);
        List<AdminUserDto> dtos = userRepository.findAll().stream()
                .map(user -> {
                    long count = subdomainRepository.countByOwner(user);
                    int max = user.computeMaxSubdomainCount(
                            subdomainProperties.getMaxPerFreeUser(),
                            subdomainProperties.getMaxPerPlusUser());
                    return AdminUserDto.builder()
                            .uuid(user.getUuid())
                            .username(user.getUsername())
                            .email(user.getEmail())
                            .isVerified(user.isVerified())
                            .tier(user.getPlan())
                            .maxSubdomainCount(max)
                            .maxSubdomainCountOverride(user.getMaxSubdomainCountOverride())
                            .subdomainCount(count)
                            .planExpiresAt(user.getPlanExpiresAt())
                            .createdAt(user.getCreatedAt())
                            .build();
                })
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @Override
    public ResponseEntity<AdminUserDetailDto> getUserDetail(String adminKey, String uuid) {
        validateKey(adminKey);
        UserEntity user = userRepository.findById(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        int max = user.computeMaxSubdomainCount(
                subdomainProperties.getMaxPerFreeUser(),
                subdomainProperties.getMaxPerPlusUser());
        List<AdminSubdomainDto> subdomains = subdomainRepository.findAllByOwner(user).stream()
                .map(this::toAdminSubdomainDto)
                .toList();
        return ResponseEntity.ok(AdminUserDetailDto.builder()
                .uuid(user.getUuid())
                .username(user.getUsername())
                .email(user.getEmail())
                .isVerified(user.isVerified())
                .tier(user.getPlan())
                .maxSubdomainCount(max)
                .maxSubdomainCountOverride(user.getMaxSubdomainCountOverride())
                .planExpiresAt(user.getPlanExpiresAt())
                .createdAt(user.getCreatedAt())
                .subdomains(subdomains)
                .build());
    }

    @Override
    public ResponseEntity<UserDto> setMaxSubdomainOverride(
            String adminKey, String uuid, Map<String, Integer> body) {
        validateKey(adminKey);
        UserEntity user = userRepository.findById(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setMaxSubdomainCountOverride(body.get("maxSubdomainCountOverride"));
        user = userRepository.save(user);
        return ResponseEntity.ok(user.toDto(user.computeMaxSubdomainCount(
                subdomainProperties.getMaxPerFreeUser(),
                subdomainProperties.getMaxPerPlusUser())));
    }

    private void validateKey(String key) {
        if (!adminProperties.getSecretKey().equals(key)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin key");
        }
    }

    private AdminSubdomainDto toAdminSubdomainDto(SubdomainEntity s) {
        return AdminSubdomainDto.builder()
                .uuid(s.getUuid())
                .label(s.getLabel())
                .fqdn(s.getFqdn())
                .targetIp(s.getTargetIp())
                .targetIpv6(s.getTargetIpv6())
                .status(s.getStatus())
                .labelMode(s.getLabelMode())
                .ownerUuid(s.getOwner().getUuid())
                .ownerUsername(s.getOwner().getUsername())
                .ownerEmail(s.getOwner().getEmail())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }
}
