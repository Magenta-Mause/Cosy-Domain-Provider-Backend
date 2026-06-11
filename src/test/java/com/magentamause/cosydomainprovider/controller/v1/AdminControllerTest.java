package com.magentamause.cosydomainprovider.controller.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.magentamause.cosydomainprovider.controller.v1.implementation.AdminController;
import com.magentamause.cosydomainprovider.entity.SubdomainEntity;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.model.action.AdminMaxSubdomainOverrideDto;
import com.magentamause.cosydomainprovider.model.action.AdminSettingsUpdateDto;
import com.magentamause.cosydomainprovider.model.action.AdminSubdomainRelabelDto;
import com.magentamause.cosydomainprovider.model.action.AdminUserUpdateDto;
import com.magentamause.cosydomainprovider.model.action.SubdomainUpdateDto;
import com.magentamause.cosydomainprovider.model.admin.*;
import com.magentamause.cosydomainprovider.model.core.*;
import com.magentamause.cosydomainprovider.services.core.GlobalSettingsService;
import com.magentamause.cosydomainprovider.services.core.SubdomainService;
import com.magentamause.cosydomainprovider.services.core.UserService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock private SubdomainService subdomainService;
    @Mock private UserService userService;
    @Mock private GlobalSettingsService globalSettingsService;

    private AdminController controller;

    private static final String VALID_KEY = "secret";

    @BeforeEach
    void setUp() {
        controller = new AdminController(subdomainService, userService, globalSettingsService);
    }

    private UserEntity user() {
        return UserEntity.builder()
                .uuid("u1")
                .username("alice")
                .email("a@a.com")
                .plan(Plan.FREE)
                .build();
    }

    private SubdomainEntity subdomain(UserEntity owner) {
        return SubdomainEntity.builder()
                .uuid("s1")
                .label("foo")
                .fqdn("foo.example.com")
                .owner(owner)
                .targetIp("1.2.3.4")
                .status(SubdomainStatus.ACTIVE)
                .labelMode(LabelMode.RANDOM)
                .build();
    }

    @Test
    void getAllSubdomains_returnsAll() {
        UserEntity u = user();
        when(subdomainService.adminGetAllSubdomains()).thenReturn(List.of(subdomain(u)));
        ResponseEntity<List<AdminSubdomainDto>> resp = controller.getAllSubdomains(VALID_KEY);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(1);
    }

    @Test
    void getSubdomain_returnsOne() {
        UserEntity u = user();
        when(subdomainService.adminGetSubdomain("s1")).thenReturn(subdomain(u));
        ResponseEntity<AdminSubdomainDto> resp = controller.getSubdomain(VALID_KEY, "s1");
        assertThat(resp.getBody().getLabel()).isEqualTo("foo");
    }

    @Test
    void updateSubdomainTargetIp_returnsUpdated() {
        UserEntity u = user();
        SubdomainUpdateDto dto = SubdomainUpdateDto.builder().targetIp("9.9.9.9").build();
        when(subdomainService.adminUpdateTargetIp("s1", dto)).thenReturn(subdomain(u));
        ResponseEntity<AdminSubdomainDto> resp =
                controller.updateSubdomainTargetIp(VALID_KEY, "s1", dto);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void relabelSubdomain_returnsRelabeled() {
        UserEntity u = user();
        AdminSubdomainRelabelDto dto = new AdminSubdomainRelabelDto();
        dto.setLabel("newlabel");
        when(subdomainService.adminRelabelSubdomain("s1", "newlabel")).thenReturn(subdomain(u));
        ResponseEntity<AdminSubdomainDto> resp = controller.relabelSubdomain(VALID_KEY, "s1", dto);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deleteSubdomain_returnsNoContent() {
        ResponseEntity<Void> resp = controller.deleteSubdomain(VALID_KEY, "s1");
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(subdomainService).adminDeleteSubdomain("s1");
    }

    @Test
    void getAllUsers_returnsList() {
        when(userService.getAllUsers()).thenReturn(List.of(user()));
        when(userService.computeMaxSubdomainCount(any())).thenReturn(1);
        when(subdomainService.getCountByOwner(any())).thenReturn(0L);
        ResponseEntity<List<AdminUserDto>> resp = controller.getAllUsers(VALID_KEY);
        assertThat(resp.getBody()).hasSize(1);
    }

    @Test
    void getUserDetail_returnsDetail() {
        UserEntity u = user();
        when(userService.getUserByUuid("u1")).thenReturn(u);
        when(subdomainService.getSubdomainsForOwner(u)).thenReturn(List.of(subdomain(u)));
        when(userService.computeMaxSubdomainCount(u)).thenReturn(1);
        ResponseEntity<AdminUserDetailDto> resp = controller.getUserDetail(VALID_KEY, "u1");
        assertThat(resp.getBody().getUuid()).isEqualTo("u1");
        assertThat(resp.getBody().getSubdomains()).hasSize(1);
    }

    @Test
    void updateUser_returnsUpdated() {
        UserEntity u = user();
        AdminUserUpdateDto dto = AdminUserUpdateDto.builder().username("new").build();
        when(userService.adminUpdateUser("u1", dto)).thenReturn(u);
        when(userService.computeMaxSubdomainCount(u)).thenReturn(1);
        when(subdomainService.getCountByOwner(u)).thenReturn(0L);
        ResponseEntity<AdminUserDto> resp = controller.updateUser(VALID_KEY, "u1", dto);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deleteUser_returnsNoContent() {
        ResponseEntity<Void> resp = controller.deleteUser(VALID_KEY, "u1");
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(userService).deleteUserByUuid("u1");
    }

    @Test
    void setMaxSubdomainOverride_returnsUser() {
        UserEntity u = user();
        when(userService.adminSetMaxSubdomainOverride("u1", 10)).thenReturn(u);
        when(userService.computeMaxSubdomainCount(u)).thenReturn(10);
        ResponseEntity<UserDto> resp =
                controller.setMaxSubdomainOverride(
                        VALID_KEY,
                        "u1",
                        AdminMaxSubdomainOverrideDto.builder()
                                .maxSubdomainCountOverride(10)
                                .build());
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getSettings_returnsDomainCreationStatus() {
        when(globalSettingsService.isDomainCreationEnabled()).thenReturn(true);
        ResponseEntity<AdminSettingsDto> resp = controller.getSettings(VALID_KEY);
        assertThat(resp.getBody().isDomainCreationEnabled()).isTrue();
    }

    @Test
    void updateSettings_disablesDomainCreation() {
        ResponseEntity<AdminSettingsDto> resp =
                controller.updateSettings(
                        VALID_KEY,
                        AdminSettingsUpdateDto.builder().domainCreationEnabled(false).build());
        assertThat(resp.getBody().isDomainCreationEnabled()).isFalse();
        verify(globalSettingsService).setDomainCreationEnabled(false);
    }
}
