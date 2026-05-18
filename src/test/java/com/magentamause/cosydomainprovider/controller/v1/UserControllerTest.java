package com.magentamause.cosydomainprovider.controller.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.magentamause.cosydomainprovider.configuration.oauth.OAuthProperties;
import com.magentamause.cosydomainprovider.configuration.subdomain.SubdomainProperties;
import com.magentamause.cosydomainprovider.controller.v1.implementation.UserController;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.model.action.UpdateUserDto;
import com.magentamause.cosydomainprovider.model.action.UserCreationDto;
import com.magentamause.cosydomainprovider.model.core.Plan;
import com.magentamause.cosydomainprovider.model.core.UserDto;
import com.magentamause.cosydomainprovider.services.auth.SecurityContextService;
import com.magentamause.cosydomainprovider.services.auth.oauth.OAuthService;
import com.magentamause.cosydomainprovider.services.core.UserService;
import com.magentamause.cosydomainprovider.services.core.UserVerificationService;
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
    @Mock private UserVerificationService userVerificationService;
    @Mock private SecurityContextService securityContextService;
    @Mock private SubdomainProperties subdomainProperties;
    @Mock private OAuthService oAuthService;
    @Mock private OAuthProperties oAuthProperties;

    private UserController controller;

    @BeforeEach
    void setUp() {
        controller =
                new UserController(
                        userService,
                        userVerificationService,
                        securityContextService,
                        subdomainProperties,
                        oAuthService,
                        oAuthProperties);
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
    void getAllUsers_returnsList() {
        List<UserEntity> users = List.of(user(), user());
        when(userService.getAllUsers()).thenReturn(users);
        ResponseEntity<List<UserDto>> resp = controller.getAllUsers();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(2);
    }

    @Test
    void createUser_returnsOk() {
        UserEntity created = user();
        UserCreationDto dto =
                UserCreationDto.builder()
                        .username("alice")
                        .email("a@a.com")
                        .password("password1")
                        .build();
        when(userService.createUser(dto)).thenReturn(created);
        doNothing().when(userVerificationService).sendInitialVerification(created);

        ResponseEntity<UserDto> resp = controller.createUser(dto);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userVerificationService).sendInitialVerification(created);
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
}
