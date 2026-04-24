package com.magentamause.cosydomainprovider.controller.v1.implementation;

import com.magentamause.cosydomainprovider.configuration.subdomain.SubdomainProperties;
import com.magentamause.cosydomainprovider.controller.v1.schema.UserApi;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.model.action.UpdateUserDto;
import com.magentamause.cosydomainprovider.model.action.UserCreationDto;
import com.magentamause.cosydomainprovider.model.core.UserDto;
import com.magentamause.cosydomainprovider.services.auth.SecurityContextService;
import com.magentamause.cosydomainprovider.services.core.UserService;
import com.magentamause.cosydomainprovider.services.core.UserVerificationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;
    private final UserVerificationService userVerificationService;
    private final SecurityContextService securityContextService;
    private final SubdomainProperties subdomainProperties;

    @Override
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(
                userService.getAllUsers().stream().map(UserEntity::toDto).toList());
    }

    @Override
    public ResponseEntity<UserDto> createUser(UserCreationDto userCreationDto) {
        UserEntity user = userService.createUser(userCreationDto);
        userVerificationService.sendInitialVerification(user);
        return ResponseEntity.ok(toDto(user));
    }

    @Override
    public ResponseEntity<UserDto> updateUser(UpdateUserDto dto) {
        UserEntity user = securityContextService.getUser();
        return ResponseEntity.ok(toDto(userService.updateUser(dto, user)));
    }

    @Override
    public ResponseEntity<Void> deleteUser() {
        String userId = securityContextService.getUserId();
        userService.deleteUserByUuid(userId);
        return ResponseEntity.noContent().build();
    }

    private UserDto toDto(UserEntity user) {
        return user.toDto(user.computeMaxSubdomainCount(
                subdomainProperties.getMaxPerFreeUser(),
                subdomainProperties.getMaxPerPlusUser()));
    }
}
