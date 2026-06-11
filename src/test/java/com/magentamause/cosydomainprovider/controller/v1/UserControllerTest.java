package com.magentamause.cosydomainprovider.controller.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.magentamause.cosydomainprovider.configuration.subdomain.SubdomainProperties;
import com.magentamause.cosydomainprovider.controller.v1.implementation.UserController;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.model.action.UpdateUserDto;
import com.magentamause.cosydomainprovider.model.core.OAuthIdentityDto;
import com.magentamause.cosydomainprovider.model.core.Plan;
import com.magentamause.cosydomainprovider.model.core.UserDto;
import com.magentamause.cosydomainprovider.services.auth.SecurityContextService;
import com.magentamause.cosydomainprovider.services.auth.oauth.OAuthService;
import com.magentamause.cosydomainprovider.services.core.UserService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserControllerTest {

    @Mock private UserService userService;
    @Mock private SecurityContextService securityContextService;
    @Mock private SubdomainProperties subdomainProperties;
    @Mock private OAuthService oAuthService;

    private UserController controller;

    @BeforeEach
    void setUp() {
        controller =
                new UserController(
                        userService, securityContextService, subdomainProperties, oAuthService);
        when(subdomainProperties.getMaxPerFreeUser()).thenReturn(1);
        when(subdomainProperties.getMaxPerPlusUser()).thenReturn(5);
    }

    private UserEntity user() {
        return UserEntity.builder()
                .uuid("u1")
                .username("alice")
                .email("a@a.com")
                .plan(Plan.FREE)
                .build();
    }

    @Test
    void updateUser_returnsUpdated() {
        UserEntity u = user();
        when(securityContextService.getUser()).thenReturn(u);
        UpdateUserDto dto = new UpdateUserDto();
        dto.setNewUsername("newname");
        when(userService.updateUser(dto, u)).thenReturn(u);

        ResponseEntity<UserDto> resp = controller.updateUser(dto);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deleteUser_returnsNoContent() {
        when(securityContextService.getUserId()).thenReturn("u1");
        ResponseEntity<Void> resp = controller.deleteUser();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userService).deleteUserByUuid("u1");
    }

    @Test
    void getLinkedIdentities_returnsOk() {
        List<OAuthIdentityDto> identities =
                List.of(OAuthIdentityDto.builder().provider("github").email("a@a.com").build());
        when(securityContextService.getUserId()).thenReturn("u1");
        when(oAuthService.getLinkedIdentities("u1")).thenReturn(identities);

        ResponseEntity<List<OAuthIdentityDto>> resp = controller.getLinkedIdentities();

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(1);
    }

    @Test
    void linkOAuthIdentity_returnsAuthorizationUrl() {
        when(securityContextService.getUserId()).thenReturn("u1");
        when(oAuthService.buildLinkAuthorizationUrl("github", "u1"))
                .thenReturn("https://github.com/login/oauth/authorize?state=link-xyz");

        ResponseEntity<String> resp = controller.linkOAuthIdentity("github");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).contains("github.com/login/oauth/authorize");
    }

    @Test
    void unlinkOAuthIdentity_returnsNoContent() {
        when(securityContextService.getUserId()).thenReturn("u1");

        ResponseEntity<Void> resp = controller.unlinkOAuthIdentity("github");

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(oAuthService).unlinkIdentity("github", "u1");
    }
}
