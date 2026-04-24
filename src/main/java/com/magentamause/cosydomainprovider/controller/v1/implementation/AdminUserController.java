package com.magentamause.cosydomainprovider.controller.v1.implementation;

import com.magentamause.cosydomainprovider.configuration.admin.AdminProperties;
import com.magentamause.cosydomainprovider.configuration.subdomain.SubdomainProperties;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.model.core.UserDto;
import com.magentamause.cosydomainprovider.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;
    private final AdminProperties adminProperties;
    private final SubdomainProperties subdomainProperties;

    @PatchMapping("/user/{uuid}/max-subdomain-override")
    public ResponseEntity<UserDto> setMaxSubdomainOverride(
            @RequestHeader("X-Admin-Key") String adminKey,
            @PathVariable String uuid,
            @RequestBody Map<String, Integer> body) {

        if (!adminProperties.getSecretKey().equals(adminKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin key");
        }

        UserEntity user = userRepository.findById(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setMaxSubdomainCountOverride(body.get("maxSubdomainCountOverride"));
        user = userRepository.save(user);

        return ResponseEntity.ok(user.toDto(user.computeMaxSubdomainCount(
                subdomainProperties.getMaxPerFreeUser(),
                subdomainProperties.getMaxPerPlusUser())));
    }
}
