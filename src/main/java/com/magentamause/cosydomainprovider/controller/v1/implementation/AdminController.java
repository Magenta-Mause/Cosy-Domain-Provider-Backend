package com.magentamause.cosydomainprovider.controller.v1.implementation;

import com.magentamause.cosydomainprovider.configuration.admin.AdminProperties;
import com.magentamause.cosydomainprovider.controller.v1.schema.AdminApi;
import com.magentamause.cosydomainprovider.entity.SubdomainEntity;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.model.action.AdminSubdomainRelabelDto;
import com.magentamause.cosydomainprovider.model.action.AdminUserUpdateDto;
import com.magentamause.cosydomainprovider.model.action.SubdomainUpdateDto;
import com.magentamause.cosydomainprovider.model.admin.AdminSubdomainDto;
import com.magentamause.cosydomainprovider.model.admin.AdminUserDetailDto;
import com.magentamause.cosydomainprovider.model.admin.AdminUserDto;
import com.magentamause.cosydomainprovider.model.core.UserDto;
import com.magentamause.cosydomainprovider.services.core.SubdomainService;
import com.magentamause.cosydomainprovider.services.core.UserService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class AdminController implements AdminApi {

    private final AdminProperties adminProperties;
    private final SubdomainService subdomainService;
    private final UserService userService;

    @Override
    public ResponseEntity<List<AdminSubdomainDto>> getAllSubdomains(String adminKey) {
        validateKey(adminKey);
        List<AdminSubdomainDto> dtos =
                subdomainService.adminGetAllSubdomains().stream()
                        .map(this::toAdminSubdomainDto)
                        .toList();
        return ResponseEntity.ok(dtos);
    }

    @Override
    public ResponseEntity<AdminSubdomainDto> getSubdomain(String adminKey, String uuid) {
        validateKey(adminKey);
        return ResponseEntity.ok(toAdminSubdomainDto(subdomainService.adminGetSubdomain(uuid)));
    }

    @Override
    public ResponseEntity<AdminSubdomainDto> updateSubdomainTargetIp(
            String adminKey, String uuid, SubdomainUpdateDto body) {
        validateKey(adminKey);
        return ResponseEntity.ok(
                toAdminSubdomainDto(subdomainService.adminUpdateTargetIp(uuid, body)));
    }

    @Override
    public ResponseEntity<AdminSubdomainDto> relabelSubdomain(
            String adminKey, String uuid, AdminSubdomainRelabelDto body) {
        validateKey(adminKey);
        return ResponseEntity.ok(
                toAdminSubdomainDto(subdomainService.adminRelabelSubdomain(uuid, body.getLabel())));
    }

    @Override
    public ResponseEntity<Void> deleteSubdomain(String adminKey, String uuid) {
        validateKey(adminKey);
        subdomainService.deleteSubdomain(uuid);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<AdminUserDto>> getAllUsers(String adminKey) {
        validateKey(adminKey);
        List<AdminUserDto> dtos =
                userService.getAllUsers().stream().map(this::toAdminUserDto).toList();
        return ResponseEntity.ok(dtos);
    }

    @Override
    public ResponseEntity<AdminUserDetailDto> getUserDetail(String adminKey, String uuid) {
        validateKey(adminKey);
        UserEntity user = userService.getUserByUuid(uuid);
        List<AdminSubdomainDto> subdomains =
                subdomainService.getSubdomainsForOwner(user).stream()
                        .map(this::toAdminSubdomainDto)
                        .toList();
        return ResponseEntity.ok(
                AdminUserDetailDto.builder()
                        .uuid(user.getUuid())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .isVerified(user.isVerified())
                        .tier(user.getPlan())
                        .maxSubdomainCount(userService.computeMaxSubdomainCount(user))
                        .maxSubdomainCountOverride(user.getMaxSubdomainCountOverride())
                        .planExpiresAt(user.getPlanExpiresAt())
                        .createdAt(user.getCreatedAt())
                        .subdomains(subdomains)
                        .build());
    }

    @Override
    public ResponseEntity<AdminUserDto> updateUser(
            String adminKey, String uuid, AdminUserUpdateDto body) {
        validateKey(adminKey);
        return ResponseEntity.ok(toAdminUserDto(userService.adminUpdateUser(uuid, body)));
    }

    @Override
    public ResponseEntity<Void> deleteUser(String adminKey, String uuid) {
        validateKey(adminKey);
        userService.deleteUserByUuid(uuid);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<UserDto> setMaxSubdomainOverride(
            String adminKey, String uuid, Map<String, Integer> body) {
        validateKey(adminKey);
        UserEntity user =
                userService.adminSetMaxSubdomainOverride(
                        uuid, body.get("maxSubdomainCountOverride"));
        return ResponseEntity.ok(user.toDto(userService.computeMaxSubdomainCount(user)));
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

    private AdminUserDto toAdminUserDto(UserEntity user) {
        return AdminUserDto.builder()
                .uuid(user.getUuid())
                .username(user.getUsername())
                .email(user.getEmail())
                .isVerified(user.isVerified())
                .tier(user.getPlan())
                .maxSubdomainCount(userService.computeMaxSubdomainCount(user))
                .maxSubdomainCountOverride(user.getMaxSubdomainCountOverride())
                .subdomainCount(subdomainService.getCountByOwner(user))
                .planExpiresAt(user.getPlanExpiresAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
