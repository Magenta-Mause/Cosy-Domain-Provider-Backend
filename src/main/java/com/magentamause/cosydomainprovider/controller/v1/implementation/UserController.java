package com.magentamause.cosydomainprovider.controller.v1.implementation;

import com.magentamause.cosydomainprovider.configuration.subdomain.SubdomainProperties;
import com.magentamause.cosydomainprovider.controller.v1.schema.UserApi;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.model.action.UpdateUserDto;
import com.magentamause.cosydomainprovider.model.core.OAuthIdentityDto;
import com.magentamause.cosydomainprovider.model.core.UserDto;
import com.magentamause.cosydomainprovider.services.auth.SecurityContextService;
import com.magentamause.cosydomainprovider.services.auth.oauth.OAuthService;
import com.magentamause.cosydomainprovider.services.core.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;
    private final SecurityContextService securityContextService;
    private final SubdomainProperties subdomainProperties;
    private final OAuthService oAuthService;

    @Override
    public ResponseEntity<UserDto> updateUser(UpdateUserDto dto) {
        UserEntity user = securityContextService.requireUser();
        return ResponseEntity.ok(toDto(userService.updateUser(dto, user)));
    }

    @Override
    public ResponseEntity<Void> deleteUser() {
        String userId = securityContextService.requireUserId();
        userService.deleteUserByUuid(userId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<OAuthIdentityDto>> getLinkedIdentities() {
        String userId = securityContextService.requireUserId();
        return ResponseEntity.ok(oAuthService.getLinkedIdentities(userId));
    }

    @Override
    public ResponseEntity<String> linkOAuthIdentity(String provider) {
        String userId = securityContextService.requireUserId();
        String url = oAuthService.buildLinkAuthorizationUrl(provider, userId);
        return ResponseEntity.ok(url);
    }

    @Override
    public ResponseEntity<Void> unlinkOAuthIdentity(String provider) {
        String userId = securityContextService.requireUserId();
        oAuthService.unlinkIdentity(provider, userId);
        return ResponseEntity.noContent().build();
    }

    private UserDto toDto(UserEntity user) {
        return user.toDto(
                user.computeMaxSubdomainCount(
                        subdomainProperties.getMaxPerFreeUser(),
                        subdomainProperties.getMaxPerPlusUser()));
    }
}
